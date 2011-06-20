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

object TrainMaxentModel {

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.TrainMaxentModel", preUsage=Some("Updown"))

  val inputFile = parser.option[String](List("i", "input"), "input", "labeled tweet input")
  val outputFile = parser.option[String](List("o", "output"), "output", "model output")

  def main(args: Array[String]) {
    try { parser.parse(args) }
    catch { case e: ArgotUsageException => println(e.message); sys.exit(0) }

    val reader = new FileReader(inputFile.value.get)
    val dataStream = new PlainTextByLineDataStream(reader)
    val eventStream = new BasicEventStream(dataStream, ",")
    val dataIndexer = new TwoPassDataIndexer(eventStream, 5)

    val model = GIS.trainModel(10, dataIndexer)

    reader.close

    val modelWriter = new BinaryGISModelWriter(model, new File(outputFile.value.get))
    modelWriter.persist
    modelWriter.close
  }
}
