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
  def after():Int

  def generateTrials(inputFile: String, nFolds: Int): Iterator[(List[GoldLabeledTweet], List[GoldLabeledTweet])] = {
    val foldsToTweets = (for ((fold, list) <- TweetFeatureReader(inputFile).zipWithIndex.groupBy((pair) => {
      val (_, index) = pair;
      index % nFolds
    })) yield {
      (fold, list.map((pair) => {
        val (tweet, _) = pair;
        tweet
      }))
    }).toList

    (for ((heldOutFold, heldOutData) <- foldsToTweets) yield {
      (heldOutData,
        foldsToTweets.filter((pair) => {
          val (listFold, _) = pair;
          listFold != heldOutFold
        }).map((pair) => {
          val (_, tweets) = pair;
          tweets
        }).flatten)
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
          logger.debug("starting run "+experimentalRun)
          val result = doExperiment(testSet, trainSet)
          logger.debug("ending run "+experimentalRun)
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