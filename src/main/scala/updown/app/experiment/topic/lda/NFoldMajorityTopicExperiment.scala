package updown.app.experiment.topic.lda

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.TopicModel
import updown.app.experiment.topic.NFoldTopicExperiment

object NFoldMajorityTopicExperiment extends NFoldTopicExperiment {

  def label(model: TopicModel, tweet: GoldLabeledTweet, goodTopic: Int, badTopic: Int): SystemLabeledTweet = {
    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    val topicDistribution = model.inferTopics(tweet)
    val sortedDist = topicDistribution.zipWithIndex.sortBy((i) => 1.0 - i._1).map((i) => i._2)
    val chosenTopic = topicDistribution.indexOf(topicDistribution.max)

    SystemLabeledTweet(id, userid, features, goldLabel,
      if (chosenTopic == goodTopic) SentimentLabel.Positive
      else if (chosenTopic == badTopic) SentimentLabel.Negative
      else SentimentLabel.Neutral
    )
  }

  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]) = {
    val labelToTopicDist = model.getLabelsToTopicDist

    //This approach will only work if there is a very clear sentiment-topic correlation.
    val badTopic = labelToTopicDist(SentimentLabel.Negative).indexOf(labelToTopicDist(SentimentLabel.Negative).max)
    val goodTopic = labelToTopicDist(SentimentLabel.Positive).indexOf(labelToTopicDist(SentimentLabel.Positive).max)
    val neutralTopic = if (labelToTopicDist.contains(SentimentLabel.Neutral)) labelToTopicDist(SentimentLabel.Neutral).indexOf(labelToTopicDist(SentimentLabel.Neutral).max) else -1
    logger.info("goodTopic:%d badTopic:%d neutralTopic:%d".format(goodTopic, badTopic, neutralTopic))

    if (goodTopic == badTopic){
      logger.error("Patholological distribution. No clear topics for bad/good labels. Exiting...")
      System.exit(1)
    } else if (neutralTopic != -1 && (badTopic == neutralTopic | goodTopic == neutralTopic)) {
      logger.warn("No clear distribution for the neutral label. ")
    }

    val res = (for (tweet <- testSet) yield {
      label(model, tweet, goodTopic, badTopic)
    }).toList
//    logger.info(res.toString)
    res
  }
}