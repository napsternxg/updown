package updown.util

import cc.mallet.types._
import updown.data.{SentimentLabel, GoldLabeledTweet}

case class Topic(prior:Double, distribution: Map[String,Double])

abstract class TopicModel {
  protected def getInstanceList(tweetList: List[GoldLabeledTweet]): (Alphabet, InstanceList) = {
    val alphabet = new Alphabet()
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
    (alphabet, instanceList)
  }

  def getTopics: List[Topic]
  def getTopicPriors: List[Double]
  def getTopicsPerInstance: List[(String,List[Double])]
  def getTopicsPerTarget: Map[SentimentLabel.Type,List[Double]]
  def inferTopics(tweet: GoldLabeledTweet): List[Double]
}