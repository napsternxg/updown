package updown.app.experiment

import updown.data.io.TweetFeatureReader
import org.clapper.argot.ArgotParser._
import org.clapper.argot.ArgotConverters._
import com.weiglewilczek.slf4s.Logging
import updown.util.Statistics
import updown.data.{SystemLabeledTweet, GoldLabeledTweet}
import java.util.Arrays
import org.clapper.argot.{MultiValueOption, ArgotUsageException, ArgotParser}

abstract class SplitExperiment extends Experiment {
  // this exists purely to make the ArgotConverters appear used to IDEA
  convertByte _
  val goldTrainSet:MultiValueOption[String] = parser.multiOption[String](List("G", "train"), "FILE", "gold labeled training data")
  val goldTestSet:MultiValueOption[String] = parser.multiOption[String](List("g", "test"), "FILE", "gold labeled test data")
  val targetsInputFileTest = parser.option[String](List("s", "targetsTest"), "targetsTestFile", "targets (TEST)")

  def doExperiment(testSet: List[GoldLabeledTweet], trainSet: List[GoldLabeledTweet]): List[SystemLabeledTweet]

  def after(): Int

  def main(args: Array[String]) = apply(args)
  
  def apply(args: Array[String]) {
    try {
      parser.parse(args)
      val trainSet: List[GoldLabeledTweet] = goldTrainSet.value.toList.flatMap((s)=>TweetFeatureReader(s))
      val testSet: List[GoldLabeledTweet] = goldTestSet.value.toList.flatMap((s)=>TweetFeatureReader(s))
      if (trainSet.length == 0) {
        parser.usage("no training instances specified")
      }
      if (testSet.length == 0) {
        parser.usage("no testing instances specified")
      }
      logger.debug("starting run")
      val result = doExperiment(testSet, trainSet)
      logger.debug("ending run")


      report(goldTrainSet.value.toString + "->" + goldTestSet.value.toString, result)
      logger.debug("running cleanup code")
      System.exit(after())
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(1)
    }
  }
}