package updown.app

import java.io._

import opennlp.tools.postag._
import opennlp.tools.sentdetect._
import opennlp.tools.tokenize._
import opennlp.tools.util._
import opennlp.maxent._
import opennlp.maxent.io._
import opennlp.model._

import org.clapper.argot._

object PerTweetEvaluator {

  val featureRowRE = """^([^|]+)\|([^|]+)\|(.*),([^,]+)$""".r

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage=Some("Updown"))

  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")

  def main(args: Array[String]) {
    try { parser.parse(args) }
    catch { case e: ArgotUsageException => println(e.message); sys.exit(0) }

    val dataInputStream = new DataInputStream(new FileInputStream(modelInputFile.value.get));
    val reader = new BinaryGISModelReader(dataInputStream)

    val model = reader.getModel

    val goldLines = scala.io.Source.fromFile(goldInputFile.value.get).getLines.toList

    var correct = 0
    var total = 0

    for(line <- goldLines) {
      val featureRowRE(tweetid, userid, featureString, goldLabel) = line
      val features = featureString.split(",")

      val result = model.eval(features)
      
      val posProb = result(0)
      val negProb = result(1)

      val systemLabel = if(posProb >= negProb) "1" else "-1"

      if(systemLabel == goldLabel) correct += 1
      
      total += 1
    }

    println("Accuracy: "+(correct.toFloat/total)+" ("+correct+"/"+total+")")
  }
}
