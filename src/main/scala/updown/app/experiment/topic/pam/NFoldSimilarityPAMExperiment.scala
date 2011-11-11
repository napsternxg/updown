package updown.app.experiment.topic.pam

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.{Statistics, TopicModel}
import updown.app.experiment.topic.NFoldTopicExperiment

object NFoldSimilarityPAMExperiment extends NFoldPAMExperiment {
  def label(model: TopicModel, tweet: GoldLabeledTweet, labelToTopicDist: Map[SentimentLabel.Type, Array[Double]]): SystemLabeledTweet = {
    val topicDistribution = model.inferTopics(tweet)
    val similarities = (for ((k, v) <- labelToTopicDist) yield (Statistics.arrayCosineSimilarity(topicDistribution, v), k)).toList.sorted.reverse
    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    val res =
      similarities match {
        case (sim, label) :: _ =>
          SystemLabeledTweet(id, userid, features, goldLabel, SentimentLabel.unitSentiment(label))
        case Nil =>
          SystemLabeledTweet(id, userid, features, goldLabel, SentimentLabel.Abstained)
      }
    logger.trace(res.toString)
    res
  }

  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]) = {
    model.getTopicPriors
    logger.debug("entering evaluation with %d items in the test set".format(testSet.length))
    val labelToTopicVector: Map[SentimentLabel.Type, Array[Double]] = model.getLabelsToTopicDist
    logger.debug("labelToTopicVector:" + labelToTopicVector.toString)
    val start = System.currentTimeMillis()
    var labeledTestSet = List[SystemLabeledTweet]()
    for ((tweet, i) <- testSet.zipWithIndex) {
      if (i % 100 == 0) {
        logger.debug("%.0f%% remaining; average label time = %fs".format((1.0 - (i + 1).toDouble / testSet.length.toDouble) * 100, (System.currentTimeMillis() - start).toDouble / (i + 1.0) / 1000.0))
      }
      if (i % 1000 == 0) {
        logger.debug("results so far:\n" + Statistics.getEvalStats("intermediate results", labeledTestSet))
      }
      labeledTestSet = label(model, tweet, labelToTopicVector) :: labeledTestSet
    }
    /*val res = Statistics.getEvalStats("Similarity Topic", labeledTestSet)
    logger.info(res.toString)
    res*/
    labeledTestSet
  }
}