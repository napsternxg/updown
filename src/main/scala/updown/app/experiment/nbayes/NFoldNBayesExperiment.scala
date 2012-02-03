package updown.app.experiment.nbayes

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.app.experiment.NFoldExperiment
import updown.util.NaiveBayesModel

object NFoldNBayesExperiment extends NFoldExperiment {
  def doExperiment(trainSet: List[GoldLabeledTweet], testSet: List[GoldLabeledTweet]) = {
    logger.info("performing Naive Bayes experiment")
    logger.debug("training model")
    val model = new NaiveBayesModel(trainSet)

    logger.debug("testing model")
    val res = testSet.map(goldTweet=>model.classify(goldTweet))
    res
  }
  def after():Int=0
}