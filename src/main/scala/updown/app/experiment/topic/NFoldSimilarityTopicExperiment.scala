package updown.app.experiment.topic

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.{Statistics, TopicModel}

object NFoldSimilarityTopicExperiment extends NFoldTopicExperiment {

  def labelNoop(model: TopicModel, tweet: GoldLabeledTweet, labelToTopicDist: Map[SentimentLabel.Type,List[Double]]): SystemLabeledTweet = {
    val topicDistribution = model.inferTopics(tweet)
    val similarities = (for ((k,v) <- labelToTopicDist) yield (Statistics.cosineSimilarity(topicDistribution, v), k)).toList.sorted.reverse
    SentimentLabel.unitSentiment(similarities(0)._2)
    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    SystemLabeledTweet(id, userid, features, goldLabel,SentimentLabel.Abstained)
  }

  def label(model: TopicModel, tweet: GoldLabeledTweet, labelToTopicDist: Map[SentimentLabel.Type,List[Double]]): SystemLabeledTweet = {
    val topicDistribution = model.inferTopics(tweet)
    val similarities = (for ((k,v) <- labelToTopicDist) yield (Statistics.cosineSimilarity(topicDistribution, v), k)).toList.sorted.reverse
    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    SystemLabeledTweet(id, userid, features, goldLabel,SentimentLabel.unitSentiment(similarities(0)._2))
  }

  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]): (Double, scala.List[(SentimentLabel.Type, Double, Double, Double)]) = {
    logger.debug("entering evaluation with %d items in the test set".format(testSet.length))
    val topicsPerTarget: Map[SentimentLabel.Type, List[Double]] = model.getTopicsPerTarget
    val start = System.currentTimeMillis()
    val res = Statistics.getEvalStats(for ((tweet,i) <- testSet.zipWithIndex) yield {
      if (i%100 == 0) {
        logger.debug("%.0f%% remaining; average label time = %fs".format((1.0-(i+1).toDouble/testSet.length.toDouble)*100, (System.currentTimeMillis()-start).toDouble/(i+1.0) /1000.0))
      }
      label(model, tweet, topicsPerTarget)
    })
    logger.debug(res.toString)
    logger.info(Statistics.reportResults(res))
    res
  }
}