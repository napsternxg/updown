package updown.util

import updown.data.{SentimentLabel, GoldLabeledTweet}
import cc.mallet.types._

case class Topic(prior: Double, distribution: Map[String, Double])

abstract class TopicModel {
  protected def getInstanceList(tweetList: List[GoldLabeledTweet]): (Alphabet, InstanceList) = {
    val alphabet = new Alphabet()
    val labelAlphabet = new Alphabet()
    val instances = (for (tweet <- tweetList) yield {
      tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          val featureSequence = new FeatureSequence(alphabet, features.length)
          for (feature <- features) {
            featureSequence.add(feature)
          }
          val label = new FeatureVector(
            labelAlphabet,
            Array[Object]("label"), Array[Double](SentimentLabel.toDouble(goldLabel)))
          new Instance(featureSequence, label, id, null)
      }
    }).toList

    val instanceList = new InstanceList(alphabet, null)
    for (instance <- instances) {
      instanceList.add(instance)
    }
    (alphabet, instanceList)
  }

  protected def getInstanceList(tweetList: List[GoldLabeledTweet], alphabet: Alphabet) = {
    val instances = (for (tweet <- tweetList) yield {
      tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          val featureSequence = new FeatureSequence(alphabet, features.length)
          for (feature <- features) {
            featureSequence.add(feature)
          }
          new Instance(featureSequence, goldLabel, id, null)
      }
    }).toList

    val instanceList = new InstanceList(alphabet, null)
    for (instance <- instances) {
      instanceList.add(instance)
    }
    instanceList
  }

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