package updown.util

import updown.data.{SentimentLabel, GoldLabeledTweet}
import cc.mallet.types._

abstract class MalletModel {
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

  val getLabelNameArray = Array[Object](
    SentimentLabel.toEnglishName(SentimentLabel.Positive),
    SentimentLabel.toEnglishName(SentimentLabel.Neutral),
    SentimentLabel.toEnglishName(SentimentLabel.Negative)
  )
  val getLabelFeatureArray: SentimentLabel.Type => Array[Double] =
    (label: SentimentLabel.Type) => {
      label match {
        case SentimentLabel.Positive => Array[Double](1.0, 0.0, 0.0)
        case SentimentLabel.Neutral => Array[Double](0.0, 1.0, 0.0)
        case SentimentLabel.Negative => Array[Double](0.0, 0.0, 1.0)
      }
    }

  protected def getInstanceListWithLabelVectors(tweetList: List[GoldLabeledTweet]): (Alphabet, InstanceList) = {
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
            getLabelNameArray,
            getLabelFeatureArray(goldLabel)
          )
          new Instance(featureSequence, label, id, null)
      }
    }).toList

    val instanceList = new InstanceList(alphabet, null)
    for (instance <- instances) {
      instanceList.add(instance)
    }
    (alphabet, instanceList)
  }
}
