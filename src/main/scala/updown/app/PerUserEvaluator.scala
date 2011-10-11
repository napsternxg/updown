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
 * This object evaluates each user's overall system positivity (fraction of tweets labeled positive by the system)
 * against his or her gold positivity (fraction of tweets truly positive), resulting in a mean squared error.
 *
 * @author Mike Speriosu
 */
object PerUserEvaluator {

  import ArgotConverters._

  val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage = Some("Updown"))

  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
  val doRandom = parser.option[String](List("r", "random"), "random", "do random")

  val POS = "POS"
  val NEG = "NEG"
  val NEU = "NEU"

  val DEFAULT_MIN_TPU = 3

  def apply(tweets: List[SystemLabeledTweet]) = evaluate(tweets)

  def computeEvaluation(tweets: scala.List[SystemLabeledTweet]): (Int, Int, Double, String) = {
    var totalError = 0.0;
    var totalErrorAlt = 0.0
    var totalNumAbstained = 0
    val usersToTweets = new scala.collection.mutable.HashMap[String, List[Tweet]] {
      override def default(s: String) = List()
    }

    val minTPU = DEFAULT_MIN_TPU

    for (tweet <- tweets) usersToTweets.put(tweet.userid, usersToTweets(tweet.userid) ::: (tweet :: Nil))

    val usersToTweetsFiltered = usersToTweets.filter(p => p._2.length >= minTPU)

    for (userid <- usersToTweetsFiltered.keys) {
      val curTweets = usersToTweetsFiltered(userid)

      var numAbstained = 0
      if (curTweets.length >= minTPU) {
        var numGoldPos = 0.0;
        var numSysPos = 0.0
        var numGoldNeg = 0.0;
        var numSysNeg = 0.0
        var numGoldNeu = 0.0;
        var numSysNeu = 0.0

        for (tweet <- curTweets) {
          tweet match {
            case SystemLabeledTweet(_, _, _, SentimentLabel.Positive, _) => numGoldPos += 1
            case SystemLabeledTweet(_, _, _, SentimentLabel.Negative, _) => numGoldNeg += 1
            case SystemLabeledTweet(_, _, _, SentimentLabel.Neutral, _) => numGoldNeu += 1
          }
          if (doRandom == None) {
            tweet match {
              case SystemLabeledTweet(_, _, _, _, SentimentLabel.Positive) => numSysPos += 1
              case SystemLabeledTweet(_, _, _, _, SentimentLabel.Negative) => numSysNeg += 1
              case SystemLabeledTweet(_, _, _, _, SentimentLabel.Neutral) => numSysNeu += 1
              case SystemLabeledTweet(_, _, _, _, null) => numAbstained += 1
            }
          } else {
            numAbstained += 1
          }
        }

        numSysPos += numAbstained.toFloat / 3
        /*if(doRandom.value != None) {
          numSysPos = numGoldPos / 2
          numAbstained = 0
        }*/
        totalError += math.pow(((numGoldPos + numGoldNeg + numGoldNeu) - (numSysPos + numSysNeg + numSysNeu)) / curTweets.length, 2)
        totalErrorAlt += math.pow(((numGoldPos) - (numSysPos)) / curTweets.length, 2)
        totalNumAbstained += numAbstained
      }
    }

    totalError /= usersToTweetsFiltered.size
    totalErrorAlt /= usersToTweetsFiltered.size

    (usersToTweetsFiltered.size, totalNumAbstained, totalError,
      "(min of " + minTPU + " tweets per user)")
  }

  def evaluate(tweets: List[SystemLabeledTweet]) = {
    val (total, abstained, error, message) = computeEvaluation(tweets)

    System.err.println("\n***** PER USER EVAL *****")

    if (abstained > 0) {
      System.err.println(abstained + " tweets were abstained on; assuming one-third (" + (abstained / 3) + ") were positive.")
    }
    System.err.println("Number of users evaluated: %d %s".format(total, message))
    if (total > 0) System.err.println("Mean squared error: %f".format(error))
    System.err.println(message)
  }

  def main(args: Array[String]) {
    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(0)
    }

    if (modelInputFile.value == None) {
      println("You must specify a model input file via -m.")
      sys.exit(0)
    }
    if (goldInputFile.value == None) {
      println("You must specify a gold labeled input file via -g.")
      sys.exit(0)
    }

    val dataInputStream = new DataInputStream(new FileInputStream(modelInputFile.value.get));
    val reader = new BinaryGISModelReader(dataInputStream)

    val model = reader.getModel

    /* Caution! Confusing code reuse below! */
    val labels = model.getDataStructures()(2).asInstanceOf[Array[String]]
    val posIndex = labels.indexOf("1")
    val negIndex = labels.indexOf("-1")
    val neuIndex = labels.indexOf("0 ")

    val goldLines = scala.io.Source.fromFile(goldInputFile.value.get, "utf-8").getLines.toList

    val tweets = TweetFeatureReader(goldInputFile.value.get)
    evaluate(for (tweet <- tweets) yield {
      tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          SystemLabeledTweet(id, userid, features, goldLabel,
            SentimentLabel.figureItOut(model.getBestOutcome(model.eval(features.toArray))))
      }
    })
  }
}
