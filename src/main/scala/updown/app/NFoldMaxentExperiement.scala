package updown.app

import org.clapper.argot.ArgotParser._
import org.clapper.argot.{ArgotUsageException, ArgotParser}
import java.io.{FileInputStream, DataInputStream}
import opennlp.maxent.io.BinaryGISModelReader
import updown.data.io.TweetFeatureReader._
import updown.data.SentimentLabel
import org.clapper.argot.ArgotConverters._
import updown.data.io.TweetFeatureReader

object NFoldMaxentExperiement {
  // this exists purely to make the ArgotConverters appear used to IDEA
  convertByte _

  def main(args: Array[String]) {
    val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage = Some("Updown"))
    val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
    val n = parser.option[Int](List("n", "folds"), "FOLDS", "the number of folds for the experiment (default 10)")

    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(0)
    }

    val nFolds: Int = n.value.getOrElse(10)

    if (goldInputFile.value == None) {
      println("You must specify a gold labeled input file via -g.")
      sys.exit(1)
    }

    val tweets = TweetFeatureReader(goldInputFile.value.get)

  }
}