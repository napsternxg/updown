package updown.app.experiment.nbayes

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.app.experiment.StaticExperiment
import java.io.{FileInputStream, DataInputStream}
import opennlp.maxent.io.BinaryGISModelReader

object StaticMaxentExperiment extends StaticExperiment {
  import org.clapper.argot.ArgotConverters._
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