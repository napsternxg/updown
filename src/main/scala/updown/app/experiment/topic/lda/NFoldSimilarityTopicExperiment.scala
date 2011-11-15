package updown.app.experiment.topic.lda

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.{Statistics, TopicModel}

object NFoldSimilarityTopicExperiment extends NFoldTopicExperiment {
  def label(model: TopicModel, tweet: GoldLabeledTweet, labelToTopicDist: Map[SentimentLabel.Type,Array[Double]]): SystemLabeledTweet = {
    val topicDistribution = model.inferTopics(tweet)
    val similarities = (for ((k,v) <- labelToTopicDist) yield (Statistics.arrayCosineSimilarity(topicDistribution, v), k)).toList.sorted.reverse
    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    SystemLabeledTweet(id, userid, features, goldLabel,SentimentLabel.unitSentiment(similarities(0)._2))
  }

  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]) = {
    logger.debug("entering evaluation with %d items in the test set".format(testSet.length))
    val start = System.currentTimeMillis()
    val labelsToTopicDist: Map[SentimentLabel.Type, Array[Double]] = model.getLabelsToTopicDist
    val res = (for ((tweet,i) <- testSet.zipWithIndex) yield {
      if (i%100 == 0) {
        logger.debug("%.0f%% remaining; average label time = %fs".format((1.0-(i+1).toDouble/testSet.length.toDouble)*100, (System.currentTimeMillis()-start).toDouble/(i+1.0) /1000.0))
      }
      label(model, tweet, labelsToTopicDist)
    }).toList
//    logger.info(res.toString)
    res
  }
}