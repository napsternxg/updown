package updown.app.experiment.topic.lda

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.TopicModel
import scala.Array
import updown.app.experiment.topic.util.MaxentDiscriminant

object SplitLDAMaxentExperiment extends SplitLDAExperiment with MaxentDiscriminant {

  def label(model: TopicModel, tweet: GoldLabeledTweet, discriminantFn: (Array[Float]) => (String, String)): SystemLabeledTweet = {
    val topicDist: Array[Float] = model.inferTopics(tweet).map((item) => item.asInstanceOf[Float])
    val (label: String, outcomes: String) = discriminantFn(topicDist)

    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    logger.trace("labeling id:%s gold:%2s with label:%2s from outcomes:%s".format(id, goldLabel.toString, label.toString, outcomes))
    SystemLabeledTweet(id, userid, features, goldLabel, SentimentLabel.figureItOut(label))
  }


  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]) = {
    logger.debug("entering evaluation with %d items in the test set".format(testSet.length))
    val labelsToTopicDists: Map[SentimentLabel.Type, List[Array[Double]]] = model.getLabelsToTopicDists
    val discriminantFn = getDiscriminantFn(labelsToTopicDists)
    val start = System.currentTimeMillis()

    val res = (for ((tweet, i) <- testSet.zipWithIndex) yield {
      if (i % 100 == 0) {
        logger.debug("%.0f%% remaining; average label time = %fs".format((1.0 - (i + 1).toDouble / testSet.length.toDouble) * 100, (System.currentTimeMillis() - start).toDouble / (i + 1.0) / 1000.0))
      }
      label(model, tweet, discriminantFn)
    }).toList
    res
  }
}