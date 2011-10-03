package updown.app

import updown.data._
import updown.data.io._

import java.io._

import opennlp.maxent.io._
import org.clapper.argot._
import ArgotConverters._

/**
 *
 * This object evaluates each tweet's system label against its gold label on a per-tweet basis, giving an accuracy
 * score.
 *
 * @author Mike Speriosu
 */
object PerTweetEvaluator {
  // this exists purely to make the ArgotConverters appear used to IDEA
  convertByte _

  def tabulate(tweets: scala.List[SystemLabeledTweet]): (Double, Int, Int, String) = {
    var correct = 0.0
    var total = 0
    var numAbstained = tweets.count(_.systemLabel == null)

    for (tweet <- tweets) {
//      println(tweet.systemLabel + "|" + tweet.goldLabel)
      /*
       * val normedTweet = tweet.normalize("alpha")
      *  val normedNormedTweet = normedTweet.normalize("int")
      *  println(normedTweet.systemLabel + "|" + normedTweet.goldLabel + "\t" + normedNormedTweet.systemLabel + "|" + normedNormedTweet.goldLabel)
      */
      //      val normedTweet = tweet.normalize("alpha")
      if (tweet.systemLabel == tweet.goldLabel) {
        correct += 1
      }

      total += 1
    }
    correct += numAbstained.toFloat / 3

    (correct, total, numAbstained,
      "Assumed one-third of the abstained results (%.2f of %d) were actually correct (this simulates the following situation: ".format(numAbstained.toFloat / 3, numAbstained) +
        "a tweet has an equal number of positive and negative features, and zero neutral features. Since more than " +
        "a third of these are actually POS or NEG (empirically), we randomy assign a label to them.")
  }

  def apply(tweets: List[SystemLabeledTweet]) = {

    val (correct, total, abstained, message) = tabulate(tweets)

    System.err.println("\n***** PER TWEET EVAL *****\n" +
      "Accuracy: %.2f (%.2f/%d)".format(correct / total, correct, total)+"\n" +
      message)
  }


  def main(args: Array[String]) {
    val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage = Some("Updown"))
    val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
    val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")

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
    val tweets = TweetFeatureReader(goldInputFile.value.get)

    apply(for (tweet <- tweets) yield {
      tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          SystemLabeledTweet(id, userid, features, goldLabel,
            SentimentLabel.figureItOut(model.getBestOutcome(model.eval(features.toArray))))
      }
    })
  }
}
