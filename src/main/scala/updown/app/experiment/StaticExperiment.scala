package updown.app.experiment

import updown.data.io.TweetFeatureReader
import org.clapper.argot.ArgotParser._
import org.clapper.argot.ArgotConverters._
import org.clapper.argot.ArgotUsageException
import updown.data.{SystemLabeledTweet, GoldLabeledTweet}

abstract class StaticExperiment extends Experiment {
  // this exists purely to make the ArgotConverters appear used to IDEA
  convertByte _
  val goldData = parser.option[String](List("g", "input"), "FILE", "gold labeled input data")

  def doExperiment(dataSet: List[GoldLabeledTweet]): List[SystemLabeledTweet]

  def after(): Int

  def main(args: Array[String]) {
    try {
      parser.parse(args)

      val dataFileName =
        goldData.value match {
          case Some(filename) => filename
          case None => parser.usage("You must specify a gold labeled input file via -g.")
        }

      logger.debug("starting run")
      val labeledTweets = doExperiment(TweetFeatureReader(dataFileName))
      logger.debug("ending run")

      report(labeledTweets)

      logger.debug("running cleanup code")
      System.exit(after())
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(1)
    }
  }
}