package updown.app

import java.io._

import model.MaxentEventStreamFactory
import model.MaxentEventStreamFactory._
import opennlp.maxent._
import opennlp.maxent.io._
import opennlp.model._

import org.clapper.argot._
import ArgotConverters._
import updown.data.GoldLabeledTweet
import updown.data.io.TweetFeatureReader

/**
 * Train a maxent model from labeled tweet input where each line has the format:
 * TWEET_ID|USER_ID|feature1,feature2,feature3,...,featureN|label
 *
 * or, using the -s flag, train from a simple file that has the format:
 * feature1,feature2,feature3,...,featureN,label
 *
 * @author Mike Speriosu
 */
object TrainMaxentModel {
  // this exists purely to make the ArgotConverters appear used to IDEA
  convertByte _

  val DEFAULT_ITERATIONS = 10
  val DEFAULT_CUTOFF = 5


  def apply(fileName: String, iterations: Int, cutoff: Int): AbstractModel = {
    GIS.PRINT_MESSAGES = false
    GIS.trainModel(MaxentEventStreamFactory(fileName), iterations, cutoff)
  }

  def apply(fileName: String): AbstractModel = apply(fileName, DEFAULT_ITERATIONS, DEFAULT_CUTOFF)

  def trainSimple(fileName: String, iterations: Int, cutoff: Int): AbstractModel = {
    val reader = new BufferedReader(new FileReader(fileName))
    val dataStream = new PlainTextByLineDataStream(reader)
    val eventStream = new BasicEventStream(dataStream, ",")

    GIS.PRINT_MESSAGES = false
    GIS.trainModel(eventStream, iterations, cutoff)
  }

  def trainWithStringIterator(iterator: Iterator[String], iterations: Int, cutoff: Int): AbstractModel = {
    GIS.PRINT_MESSAGES = false

    GIS.trainModel(MaxentEventStreamFactory.getWithStringIterator(iterator), iterations, cutoff)
  }

  //  def apply[String](iterator: Iterator[String]): AbstractModel = apply(iterator, DEFAULT_ITERATIONS, DEFAULT_CUTOFF)

  def trainWithGoldLabeledTweetIterator(iterator: Iterator[GoldLabeledTweet], iterations: Int, cutoff: Int): AbstractModel = {
    GIS.PRINT_MESSAGES = false
    GIS.trainModel(MaxentEventStreamFactory.getWithGoldLabeledTweetIterator(iterator), iterations, cutoff)
  }

  def trainWithGoldLabeledTweetIterator(iterator: Iterator[GoldLabeledTweet]): AbstractModel = trainWithGoldLabeledTweetIterator(iterator, DEFAULT_ITERATIONS, DEFAULT_CUTOFF)

  def main(args: Array[String]) {
    val parser = new ArgotParser("updown run updown.app.TrainMaxentModel", preUsage = Some("Updown"))
    val inputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled tweet input")
    val outputFile = parser.option[String](List("m", "output"), "output", "model output") //Matt votes to change abbrev from "m" to "o"...
    val iterations = parser.option[Int](List("n", "iterations"), "iterations", "number of iterations (default = " + DEFAULT_ITERATIONS + ")")
    val cutoff = parser.option[Int](List("c", "cutoff"), "cutoff", "number of times a feature must be seen to be used (default = " + DEFAULT_CUTOFF + ")")
    val simple = parser.option[String](List("s", "simple"), "simple", "read tweets in simple format, without userid and tweetid")

    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(0)
    }

    if (inputFile.value == None) {
      println("You must specify a labeled tweet input file via -g.") // changed "-i" to "-g"
      sys.exit(0)
    }
    if (outputFile.value == None) {
      println("You must specify a model output file via -m.")
      sys.exit(0)
    }


    val model: AbstractModel = if (simple.value == None)
      apply(inputFile.value.get, iterations.value.getOrElse(DEFAULT_ITERATIONS), cutoff.value.getOrElse(DEFAULT_CUTOFF))
    else
      trainSimple(inputFile.value.get, iterations.value.getOrElse(DEFAULT_ITERATIONS), cutoff.value.getOrElse(DEFAULT_CUTOFF))

    val modelWriter = new BinaryGISModelWriter(model, new File(outputFile.value.get))
    modelWriter.persist()
    modelWriter.close()
  }
}
