package updown.app

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.{Statistics, LDATopicModel, TopicModel}

object NFoldTopicExperiment extends NFoldExperiment {


  def label(model: TopicModel, tweet: GoldLabeledTweet, goodTopic: Int, badTopic: Int): SystemLabeledTweet = {
    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    val topicDistribution = model.inferTopics(tweet)
    val sortedDist = topicDistribution.zipWithIndex.sortBy((i) => 1.0 - i._1).map((i) => i._2)

    SystemLabeledTweet(id, userid, features, goldLabel,
      if (goodTopic == -1 || badTopic == -1) {
        assert(goodTopic == badTopic)
        SentimentLabel.Abstained
      }
      else if (sortedDist(0) == goodTopic) SentimentLabel.Positive
      else if (sortedDist(0) == badTopic) SentimentLabel.Negative
      else if (sortedDist(1) == goodTopic) SentimentLabel.Positive
      else if (sortedDist(1) == badTopic) SentimentLabel.Negative
      else if (sortedDist(2) == goodTopic) SentimentLabel.Positive
      else SentimentLabel.Negative
    )
  }

  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]): (Double, scala.List[(SentimentLabel.Type, Double, Double, Double)]) = {
    val labelToTopicDist = model.getTopicsPerTarget
    val badDist = labelToTopicDist(SentimentLabel.Negative).zipWithIndex.sortBy((i) => 1.0 - i._1).map((i) => i._2)
    val goodDist = labelToTopicDist(SentimentLabel.Positive).zipWithIndex.sortBy((i) => 1.0 - i._1).map((i) => i._2)
    val goodTopic = goodDist(0)
    val badTopic = if (goodTopic != badDist(0)) badDist(0) else badDist(1)

    val res = Statistics.getEvalStats(for (tweet <- testSet) yield {
      label(model, tweet, goodTopic, badTopic)
    })
    logger.debug(Statistics.getEvalStats(for (tweet <- testSet) yield {
      label(model, tweet, goodTopic, badTopic)
    }).toString)
    logger.info(Statistics.reportResults(res))
    res
  }

  def doExperiment(testSet: List[GoldLabeledTweet], trainSet: List[GoldLabeledTweet]) = {
    val model: TopicModel = new LDATopicModel(trainSet, 3, 1000, 100, 0.1)

    logger.info("topic distribution:\n     :" + model.getTopicPriors)
    logger.info({
      val labelToTopicDist = model.getTopicsPerTarget
      "topic distribution over labels:\n" + (for ((k, v) <- labelToTopicDist) yield "%5s:%s".format(k, v)).mkString("\n")
    })
    logger.info({
      val topics = model.getTopics
      "topic distributions\n" +
        (for (i <- 0 until 3) yield "%5s: Topic(%s,%s)".format(i, topics(i).prior, topics(i).distribution.toList.sortBy((pair) => (1 - pair._2)))).mkString("\n")
    })
    evaluate(model, testSet)
  }
}