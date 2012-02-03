package updown.util

import updown.data.{SentimentLabel, GoldLabeledTweet}
import cc.mallet.types._

case class Topic(prior: Map[String, Double], distribution: Map[String, Double])

abstract class TopicModel extends MalletModel {

  def getTopics: List[Topic]

  def getTopicPriors: Array[Double]

  def getIdsToTopicDist: Map[String, Array[Double]]

  def getLabelsToTopicDists: Map[SentimentLabel.Type, List[Array[Double]]]

  def getLabelsToTopicDist: Map[SentimentLabel.Type, Array[Double]] = {
    (for ((label, topicDist: List[Array[Double]]) <- getLabelsToTopicDists) yield {
      val N = topicDist.length
      (label,
        topicDist
          .reduce((a: Array[Double], b: Array[Double]) => (a zip b).map {
          case (x, y) => x + y
        })
          .map(_ / N)
        )
    }).toMap
  }

  def inferTopics(tweet: GoldLabeledTweet): Array[Double]

  def save(filename: String)
}