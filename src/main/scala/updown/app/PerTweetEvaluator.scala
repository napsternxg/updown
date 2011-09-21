package updown.app

import model.EvaluationResult
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
  val NEU = "NEU"

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage=Some("Updown"))

  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")

  def apply(tweets: List[Tweet]) = evaluate(tweets)

  def computeEvaluation(tweets: scala.List[Tweet]): (Double, Int, Int, String) =  {
    var correct = 0.0
    var total = 0
    var numAbstained = tweets.count(_.systemLabel == null)

    for (tweet <- tweets) {
      println(tweet.systemLabel + "|" + tweet.goldLabel)
      /*
       * val normedTweet = tweet.normalize("alpha")
      *  val normedNormedTweet = normedTweet.normalize("int")
      *  println(normedTweet.systemLabel + "|" + normedTweet.goldLabel + "\t" + normedNormedTweet.systemLabel + "|" + normedNormedTweet.goldLabel)
      */
      val normedTweet = tweet.normalize("alpha")
      if (normedTweet.systemLabel == tweet.goldLabel) {
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

  def evaluate(tweets: List[Tweet]) = {
    val (correct, total, abstained, message) = computeEvaluation(tweets)

    println("\n***** PER TWEET EVAL *****")
    println("Accuracy: %.2f (%.2f/%d)".format(correct/total,correct,total))
    println(message)
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

    val labels = model.getDataStructures()(2).asInstanceOf[Array[String]]
    val posIndex = labels.indexOf("1")
    val negIndex = labels.indexOf("-1")
    val neuIndex = labels.indexOf("0")
        
    val goldLines = scala.io.Source.fromFile(goldInputFile.value.get,"utf-8").getLines.toList

    val tweets = TweetFeatureReader(goldInputFile.value.get)
    
    for(tweet <- tweets) {

      val result = model.eval(tweet.features.toArray)
      val posProb = if(posIndex >= 0) result(posIndex) else 0.0
      val negProb = if(negIndex >= 0) result(negIndex) else 0.0
      val neuProb = if (neuIndex >= 0) result(neuIndex) else 0.0


      if(posProb >= negProb && posProb >= neuProb) tweet.systemLabel = "1"
      else if(negProb >= posProb && negProb >= neuProb) tweet.systemLabel = "-1" 
      else if(neuProb >= posProb && neuProb >= negProb) tweet.systemLabel = "0"

    }

    evaluate(tweets)
  }
}
