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
object PerTweetEvaluator {

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage=Some("Updown"))

  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")

  def evaluate(tweets: List[Tweet]) = {
    var correct = 0
    var total = 0

    for(tweet <- tweets) {
      if(tweet.systemLabel == tweet.goldLabel) {
        correct += 1
      }
      total += 1
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

    var correct = 0
    var total = 0

    for(tweet <- TweetFeatureReader(goldInputFile.value.get)) {
      val result = model.eval(tweet.features.toArray)
      
      val posProb = result(0)
      val negProb = result(1)

      val systemLabel = if(posProb >= negProb) "POS" else "NEG"

      if(systemLabel == tweet.goldLabel) correct += 1
      
      total += 1
    }

    println("Accuracy: "+(correct.toFloat/total)+" ("+correct+"/"+total+")")
  }
}
