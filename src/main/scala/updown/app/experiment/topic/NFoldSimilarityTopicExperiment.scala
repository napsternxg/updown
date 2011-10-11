package updown.app.experiment.topic

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.{Statistics, TopicModel}

object NFoldSimilarityTopicExperiment extends NFoldTopicExperiment {

  def label(model: TopicModel, tweet: GoldLabeledTweet, labelToTopicDist: Map[SentimentLabel.Type,List[Double]]): SystemLabeledTweet = {
    val topicDistribution = model.inferTopics(tweet)
    logger.debug("inferred topicDist: "+topicDistribution.toString)
    val similarities = (for ((k,v) <- labelToTopicDist) yield (Statistics.cosineSimilarity(topicDistribution, v), k)).toList.sorted.reverse
    logger.debug("similarities: "+similarities.toString)
    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet

    SystemLabeledTweet(id, userid, features, goldLabel,similarities(0)._2)
  }

  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]): (Double, scala.List[(SentimentLabel.Type, Double, Double, Double)]) = {
    val res = Statistics.getEvalStats(for (tweet <- testSet) yield {
      label(model, tweet, model.getTopicsPerTarget)
    })
    logger.debug(res.toString)
    logger.info(Statistics.reportResults(res))
    res
  }
}