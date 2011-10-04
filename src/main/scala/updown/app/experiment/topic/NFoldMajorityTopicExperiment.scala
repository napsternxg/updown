package updown.app.experiment.topic

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.{Statistics, LDATopicModel, TopicModel}

object NFoldMajorityTopicExperiment extends NFoldTopicExperiment {

  def label(model: TopicModel, tweet: GoldLabeledTweet, goodTopic: Int, badTopic: Int): SystemLabeledTweet = {
    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    val topicDistribution = model.inferTopics(tweet)
    val sortedDist = topicDistribution.zipWithIndex.sortBy((i) => 1.0 - i._1).map((i) => i._2)

    // for now, we'll always guess positive or negative, never neutral
    SystemLabeledTweet(id, userid, features, goldLabel,
      if (goodTopic == badTopic) SentimentLabel.Abstained
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
    logger.debug("badDist: "+badDist.toString)
    val goodDist = labelToTopicDist(SentimentLabel.Positive).zipWithIndex.sortBy((i) => 1.0 - i._1).map((i) => i._2)
    logger.debug("goodDist: "+goodDist.toString)

    val (goodTopic, badTopic, neutralTopic): (Int, Int, Int) =
    if (labelToTopicDist.contains(SentimentLabel.Neutral)) {
      val neutralDist = labelToTopicDist(SentimentLabel.Neutral).zipWithIndex.sortBy((i) => 1.0 - i._1).map((i) => i._2)
      logger.debug("neutralDist: "+neutralDist.toString)
      val neutralTopic = neutralDist(0)
        if (goodDist(0) != neutralTopic) {
          if (goodDist(0) != badDist(0)) {
            (goodDist(0), neutralDist, badDist(0))
          } else {
            //then we have a pathological case
            logger.warn("pathological topic distribution: %s".format(labelToTopicDist.toString))
            (-1, -1, -1)
          }
        } else {
          val goodTopic = goodDist(1)
          val badTopic =
          if (badDist(0) != neutralTopic){
             badDist(0)
          } else {
            badDist(1)
          }
          if (goodTopic == badTopic){
            // then we have a pathological case
            logger.warn("pathological topic distribution: %s".format(labelToTopicDist.toString))
            (-1, -1, -1)
          } else {
            (goodTopic, neutralTopic, badTopic)
          }
        }
      } else {
        // there were no neutral training instances
        if (goodDist(0) == badDist(0)) {
          val neutralTopic = goodDist(0)
          if (goodDist(1) == badDist(1)) {
            // then we have a pathological case, and the topics are not sentimental
            logger.warn("pathological topic distribution: %s".format(labelToTopicDist.toString))
            (-1, -1, -1)
          } else {
            // then the neutral topic was dominant in both cases, and the second topic held the sentiment
            (goodDist(1), neutralTopic, badDist(1))
          }
        } else {
          // then the sentimental topic was dominant, and we just have to find the neutral topic
          val goodTopic = goodDist(0)
          val badTopic = badDist(0)
          if (goodDist(1) != badTopic) {
            (goodTopic, goodDist(1), badTopic)
          } else {
            (goodTopic, goodDist(2), badTopic)
          }
        }
      }
    assert ((goodTopic == -1 && badTopic == -1 && neutralTopic == -1) ||
      (goodTopic != badTopic && badTopic != neutralTopic && goodTopic != neutralTopic))
    logger.info("goodTopic:%d badTopic:%d neutralTopic:%d".format(goodTopic, badTopic, neutralTopic))

    val res = Statistics.getEvalStats(for (tweet <- testSet) yield {
      label(model, tweet, goodTopic, badTopic)
    })
    logger.debug(res.toString)
    logger.info(Statistics.reportResults(res))
    res
  }
}