package updown.app.experiment.topic.pam

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.TopicModel
import updown.app.experiment.topic.util.MaxentDiscriminant
import java.util.Arrays

object NFoldDiscriminantPAMExperiment extends NFoldPAMExperiment with MaxentDiscriminant {
  def label(model: TopicModel, tweet: GoldLabeledTweet, discriminantFn: (Array[Float]) => (String, String)): SystemLabeledTweet = {
    val topicDist: Array[Float] = model.inferTopics(tweet).map((item) => item.asInstanceOf[Float])
    val (label: String, outcomes: String) = discriminantFn(topicDist)

    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    logger.trace("labeling id:%s gold:%s with label:%s from outcomes:%s".format(id, goldLabel.toString, label.toString, outcomes))
    SystemLabeledTweet(id, userid, features, goldLabel, SentimentLabel.figureItOut(label))
  }

  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]) = {
    logger.debug("entering evaluation with %d items in the test set".format(testSet.length))
    val labelsToTopicDists: Map[SentimentLabel.Type, List[Array[Double]]] = model.getLabelsToTopicDists
    logger.debug({
      val tmp = model.getLabelsToTopicDist
      "Average distributions:\n"+(for ((label,dist)<- tmp) yield {
        "\t"+label.toString + ": "+Arrays.toString(dist)
      }).mkString("\n")
    })
    val discriminantFn = getDiscriminantFn(labelsToTopicDists)

    val start = System.currentTimeMillis()

    (for ((tweet, i) <- testSet.zipWithIndex) yield {
      if (i % 100 == 0) {
        logger.debug("%.0f%% remaining; average label time = %fs".format((1.0 - (i + 1).toDouble / testSet.length.toDouble) * 100, (System.currentTimeMillis() - start).toDouble / (i + 1.0) / 1000.0))
      }
      label(model, tweet, discriminantFn)
    }).toList
  }
}