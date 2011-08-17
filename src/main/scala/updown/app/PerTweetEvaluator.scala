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
  val NEU = "NEU"

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
//      println(tweet.goldLabel.getClass.toString)
      if(tweet.systemLabel == tweet.goldLabel.toString.trim) {
        correct += 1
      }
//      else println(tweet.systemLabel + "|" + tweet.goldLabel)
      total += 1
    }

    println("\n***** PER TWEET EVAL *****")

    if(numAbstained > 0) {
      correct += numAbstained.toFloat / 3
      println(numAbstained + " tweets were abstained on; assuming one-third (" + (numAbstained.toFloat/3) + ") were correct.")
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

    val labels = model.getDataStructures()(2).asInstanceOf[Array[String]]
    val posIndex = labels.indexOf("1 ")
    val negIndex = labels.indexOf("-1 ")
    val neuIndex = labels.indexOf("0 ")
    var test = (labels(0).toString == "0 " || labels(0).toString == "-1 " || labels(0).toString == "1 ") 
    println(posIndex + " " + negIndex + " " + neuIndex)
    println(labels(0) + " " + labels(1) + " " + labels(2))
    println(test)
//    for (i <- labels) println(i)

    
    val goldLines = scala.io.Source.fromFile(goldInputFile.value.get,"utf-8").getLines.toList

    val tweets = TweetFeatureReader(goldInputFile.value.get)
    
    for(tweet <- tweets) {
     //println(tweet.toString)
      val result = model.eval(tweet.features.toArray)
//      val classOfResult = result.getClass.toString      
//      println("==========\nClass: "+ classOfResult + "\n==========") //tried to get at the type but failed

      
      val posProb = if(posIndex >= 0) result(posIndex) else 0.0
      val negProb = if(negIndex >= 0) result(negIndex) else 0.0
      val neuProb = if (negIndex >= 0) result(neuIndex) else 0.0

//      println("posProb: "+posProb+"\t negProb: "+negProb+"\t neuProb: "+neuProb)
//      println("resPos: "+result(posIndex)+"\t resNeg: "+result(negIndex)+"\t resNeu: "+result(neuIndex))
      

      if(posProb >= negProb && posProb >= neuProb) tweet.systemLabel = "1"//POS
      else if(negProb >= posProb && negProb >= neuProb) tweet.systemLabel = "-1" //NEG
      else if(neuProb >= posProb && neuProb >= negProb) tweet.systemLabel = "0" //NEU
      
//      println("tweet.systemLabel is... " + tweet.systemLabel)

    }

    evaluate(tweets)
  }
}
