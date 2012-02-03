package updown.app.experiment

import updown.data.io.TweetFeatureReader
import org.clapper.argot.ArgotParser._
import org.clapper.argot.ArgotConverters._
import com.weiglewilczek.slf4s.Logging
import updown.util.Statistics
import org.clapper.argot.{ArgotUsageException, ArgotParser}
import updown.data.{SystemLabeledTweet, GoldLabeledTweet}
import java.util.Arrays

abstract class SplitExperiment extends Experiment {
  // this exists purely to make the ArgotConverters appear used to IDEA
  convertByte _
  val goldTrainSet = parser.option[String](List("G", "train"), "FILE", "gold labeled training data")
  val goldTestSet = parser.option[String](List("g", "test"), "FILE", "gold labeled test data")
  val targetsInputFileTest = parser.option[String](List("s", "targetsTest"), "targetsTestFile", "targets (TEST)")

  def doExperiment(testSet: List[GoldLabeledTweet], trainSet: List[GoldLabeledTweet]): List[SystemLabeledTweet]

  def after(): Int

  def main(args: Array[String]) {
    try {
      parser.parse(args)
      val trainFileName =
        goldTrainSet.value match {
          case Some(filename) => filename
          case None => parser.usage("You must specify a gold labeled training file")
        }
      val testFileName =
        goldTestSet.value match {
          case Some(filename) => filename
          case None => parser.usage("You must specify a gold labeled test file via")
        }
      val result =
      {
          logger.debug("starting run")
          val result = doExperiment(TweetFeatureReader(testFileName), TweetFeatureReader(trainFileName))
          logger.debug("ending run")
          result
      }
      
      report(trainFileName.toString+"->"+testFileName.toString ,result)
      logger.debug("running cleanup code")
      System.exit(after())
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(1)
    }
  }
}