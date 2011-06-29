package updown.app

import updown.data._
import updown.data.io._

import java.io._

import opennlp.maxent._
import opennlp.maxent.io._
import opennlp.model._

import org.clapper.argot._

/**
 *
 * @author Mike Speriosu
 */
object PerUserEvaluator {

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage=Some("Updown"))

  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")

  val POS = "POS"
  val NEG = "NEG"

  val DEFAULT_MIN_TPU = 1

  def apply(tweets: List[Tweet]) = evaluate(tweets)

  def evaluate(tweets: List[Tweet]) = {
    var totalError = 0.0
    var totalNumAbstained = 0
    val usersToTweets = new scala.collection.mutable.HashMap[String, List[Tweet]] { override def default(s: String) = List() }

    val minTPU = DEFAULT_MIN_TPU

    for(tweet <- tweets)
      usersToTweets.put(tweet.userid, usersToTweets(tweet.userid) ::: (tweet :: Nil))

    for(userid <- usersToTweets.keys) {
      val curTweets = usersToTweets(userid)

      var numAbstained = 0
      if(curTweets.length >= minTPU) {
        var numGoldPos = 0
        var numSysPos = 0.0
        for(tweet <- curTweets) {
          if(tweet.goldLabel == POS) numGoldPos += 1
          if(tweet.systemLabel == POS) numSysPos += 1
          else if(tweet.systemLabel == null) numAbstained += 1
        }

        numSysPos += numAbstained.toFloat / 2
        totalError += math.pow((numGoldPos - numSysPos) / curTweets.length, 2)
        totalNumAbstained += numAbstained
      }
    }

    totalError /= usersToTweets.size

    println("\n***** PER USER EVAL *****")

    if(totalNumAbstained > 0)
      println(totalNumAbstained + " tweets were abstained on; assuming half (" + (totalNumAbstained.toFloat/2) + ") were positive.")

    println("Number of users evaluated: " + usersToTweets.size + " (min of " + minTPU + " tweets per user)")
    println("Mean squared error: " + totalError)
  }

  def main(args: Array[String]) {
    try { parser.parse(args) }
    catch { case e: ArgotUsageException => println(e.message); sys.exit(0) }

    if(modelInputFile.value == None) {
      println("You must specify a model input file via -m.")
      sys.exit(0)
    }
    if(goldInputFile.value == None) {
      println("You must specify a gold labeled input file via -g.")
      sys.exit(0)
    }

    val dataInputStream = new DataInputStream(new FileInputStream(modelInputFile.value.get));
    val reader = new BinaryGISModelReader(dataInputStream)

    val model = reader.getModel

    val goldLines = scala.io.Source.fromFile(goldInputFile.value.get).getLines.toList

    val tweets = TweetFeatureReader(goldInputFile.value.get)

    for(tweet <- tweets) {
      val result = model.eval(tweet.features.toArray)
      
      val posProb = result(0)
      val negProb = result(1)

      tweet.systemLabel = if(posProb >= negProb) POS else NEG
    }

    evaluate(tweets)
  }
}
