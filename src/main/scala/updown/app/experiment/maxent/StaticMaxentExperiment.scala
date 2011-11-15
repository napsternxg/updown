package updown.app.experiment.maxent

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.app.TrainMaxentModel
import updown.app.experiment.{StaticExperiment, SplitExperiment}
import java.io.{FileInputStream, DataInputStream}
import opennlp.maxent.io.BinaryGISModelReader
import org.clapper.argot.ArgotConverters._

object StaticMaxentExperiment extends StaticExperiment {
  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")

  def doExperiment(testSet: List[GoldLabeledTweet]) = {
    logger.info("performing Maxent experiment")
    logger.debug("loading model")
    val model =
      modelInputFile.value match {
        case Some(filename) =>
          new BinaryGISModelReader(new DataInputStream(new FileInputStream(modelInputFile.value.get))).getModel
        case None =>
          parser.usage("You must specify a model input file")
      }

    logger.debug("testing model")
    val res = for (tweet <- testSet) yield {
      tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          SystemLabeledTweet(id, userid, features, goldLabel,
            SentimentLabel.figureItOut(model.getBestOutcome(model.eval(features.toArray))))
      }
    }
    res
  }

  def after(): Int = 0
}