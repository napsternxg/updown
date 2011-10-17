package updown.app.experiment

import updown.data.io.TweetFeatureReader
import updown.data.{SentimentLabel, GoldLabeledTweet}
import org.clapper.argot.ArgotParser._
import org.clapper.argot.ArgotConverters._
import com.weiglewilczek.slf4s.Logging
import updown.util.Statistics
import org.clapper.argot.{SingleValueOption, ArgotUsageException, ArgotParser}

abstract class NFoldExperiment extends Logging {
  // this exists purely to make the ArgotConverters appear used to IDEA
  convertByte _
  val parser = new ArgotParser(this.getClass.getName, preUsage = Some("Updown"))
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
  val n = parser.option[Int](List("n", "folds"), "FOLDS", "the number of folds for the experiment (default 10)")
  var experimentalRun = 0

  def doExperiment(testSet: List[GoldLabeledTweet], trainSet: List[GoldLabeledTweet]):
  (Double, List[(updown.data.SentimentLabel.Type, Double, Double, Double)])

  def after(): Int

  def generateTrials(inputFile: String, nFolds: Int): Iterator[(List[GoldLabeledTweet], List[GoldLabeledTweet])] = {
    val polToTweetLists = TweetFeatureReader(inputFile).groupBy((tweet) => tweet.goldLabel)

    val minListLength = (for ((pol, tweetList) <- polToTweetLists) yield tweetList.length).min
    logger.info("takining %d items from each polarity class. This was the minimum number in any class".format(minListLength))
    val allTweetsFolded =
      (for (index <- 0 until minListLength) yield {
          (for ((pol, tweetList) <- polToTweetLists) yield {
            (pol, index, (index % nFolds, tweetList(index)))
          }).toList.map{case(pol,index,item)=>item}
          // this is really strange. If I just emit the item, it only emits every nth one.
          // Somehow, emitting a tuple and then unmapping it fixes the problem.
          // I'm guessing this is because the input is a map, and it is trying to make the output a map as well.
      }).toList.flatten

    val foldsToTweets = allTweetsFolded.groupBy{case(fold, tweet) => fold}
      .map{case(fold,list)=>(fold,list.map{case(fold,tweet)=>tweet})}

    (for ((heldOutFold, heldOutData) <- foldsToTweets) yield {
      (heldOutData, foldsToTweets.filter{case(setNo,list)=>setNo != heldOutFold}.map{case(setNo,list)=>list}.flatten.toList)
    }).iterator
  }

  def main(args: Array[String]) {
    try {
      parser.parse(args)

      val nFolds: Int = n.value.getOrElse(10)

      if (goldInputFile.value == None) {
        parser.usage("You must specify a gold labeled input file via -g.")
      }

      val inputFile = goldInputFile.value.get
      val results =
        (for ((testSet, trainSet) <- generateTrials(inputFile, nFolds)) yield {
          experimentalRun += 1
          logger.debug("starting run " + experimentalRun)
          val result = doExperiment(testSet, trainSet)
          logger.debug("ending run " + experimentalRun)
          result
        }).toList

      logger.info("intermediate results:\n" + results.mkString("\n"))
      println("\n" + Statistics.reportResults(Statistics.averageResults(results)))
      logger.debug("running cleanup code")
      System.exit(after())
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(1)
    }
  }
}