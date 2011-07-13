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
 * This object evaluates each tweet's system label against its gold label on a per-tweet basis, giving an accuracy
 * score.
 * 
 * @author Mike Speriosu
 */
object PerTweetEvaluator {

  val POS = "POS"
  val NEG = "NEG"

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage=Some("Updown"))

  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")

  def apply(tweets: List[Tweet]) = evaluate(tweets)

  def evaluate(tweets: List[Tweet]) = {
    var correct = 0.0
    var total = 0
    var numAbstained = tweets.count(_.systemLabel == null)

    for(tweet <- tweets) {
      if(tweet.systemLabel == tweet.goldLabel) {
        correct += 1
      }
      total += 1
    }

    println("\n***** PER TWEET EVAL *****")

    if(numAbstained > 0) {
      correct += numAbstained.toFloat / 2
      println(numAbstained + " tweets were abstained on; assuming half (" + (numAbstained.toFloat/2) + ") were correct.")
    }
    println("Accuracy: "+(correct.toFloat/total)+" ("+correct+"/"+total+")")
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
