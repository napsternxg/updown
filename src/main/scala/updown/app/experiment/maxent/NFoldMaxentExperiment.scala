package updown.app.experiment.maxent

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.Statistics
import updown.app.experiment.NFoldExperiment
import updown.app.TrainMaxentModel

object NFoldMaxentExperiment extends NFoldExperiment {
  def doExperiment(testSet: List[GoldLabeledTweet], trainSet: List[GoldLabeledTweet]) = {
    logger.info("performing Maxent experiment")
    logger.debug("training model")
    val model = TrainMaxentModel.trainWithGoldLabeledTweetIterator(trainSet.iterator)

    logger.debug("testing model")
    val res = Statistics.getEvalStats(for (tweet <- testSet) yield {
      tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          SystemLabeledTweet(id, userid, features, goldLabel,
            SentimentLabel.figureItOut(model.getBestOutcome(model.eval(features.toArray))))
      }
    })
    logger.info(Statistics.reportResults(res))
    res
  }
  def after():Int=0
}