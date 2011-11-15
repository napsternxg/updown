package updown.app.experiment.topic.lda

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.TopicModel
import scala.Array
import updown.app.experiment.topic.util.KNNDiscriminant
import org.clapper.argot.ArgotConverters._
object NFoldKNNDiscriminantExperiment extends NFoldTopicExperiment with KNNDiscriminant {
  val DEFAULT_K = 11
  val kOption = parser.option[Int](List("k","numNearestNeighbors"), "INT", "the number of nearest neighbors to consider in choosing a label")

  def label(model: TopicModel, tweet: GoldLabeledTweet, discriminantFn: (Array[Float]) => (String, String)): SystemLabeledTweet = {
    val topicDist: Array[Float] = model.inferTopics(tweet).map((item) => item.asInstanceOf[Float])
    val (label: String, outcomes: String) = discriminantFn(topicDist)

    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    logger.trace("labeling id:%s gold:%2s with label:%2s from outcomes:%s".format(id, goldLabel.toString, label.toString, outcomes))
    SystemLabeledTweet(id, userid, features, goldLabel, SentimentLabel.figureItOut(label))
  }


  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]) = {
    logger.debug("entering evaluation with %d items in the test set".format(testSet.length))
    val k = kOption.value match {
      case Some(x:Int) => x
      case None => DEFAULT_K
    }

    val labelsToTopicDists: Map[SentimentLabel.Type, List[Array[Double]]] = model.getLabelsToTopicDists
    val discriminantFn = getDiscriminantFn(k,labelsToTopicDists)
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