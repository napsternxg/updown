package updown.app.experiment.maxent

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.Statistics
import updown.app.TrainMaxentModel
import updown.app.experiment.{SplitExperiment, NFoldExperiment}

object SplitMaxentExperiment extends SplitExperiment with MaxentModel {
  def doExperiment(testSet: List[GoldLabeledTweet], trainSet: List[GoldLabeledTweet]) = {
    logger.info("performing Maxent experiment")
    logger.debug("training model")
    val sigma = sigmaOption.value match {
      case Some(sigma: Double) => sigma
      case _ => 0.0
    }
    val iterations = iterationsOption.value match {
      case Some(iterations: Int) => iterations
      case _ => TrainMaxentModel.DEFAULT_ITERATIONS
    }
    val model = TrainMaxentModel.trainWithGoldLabeledTweetIterator(
      trainSet.iterator,
      iterations,
      TrainMaxentModel.DEFAULT_CUTOFF,
      sigma)

    print("testing model")
    var n=0
    val res = for (tweet <- testSet) yield {
      n+=1
      tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          SystemLabeledTweet(id, userid, features, goldLabel,
            SentimentLabel.figureItOut(model.getBestOutcome(model.eval(features.toArray))))
      }
    }
    print(n)
    res
  }

  def after(): Int = 0
}