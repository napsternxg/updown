package updown.util

import cc.mallet.classify.NaiveBayesTrainer
import updown.data.SystemLabeledTweet._
import updown.data.{SystemLabeledTweet, SentimentLabel, GoldLabeledTweet}
import cc.mallet.types._

class NaiveBayesModel(tweets: List[GoldLabeledTweet]) extends MalletModel {
  override protected def getInstanceList(tweetList: List[GoldLabeledTweet]): (Alphabet, InstanceList) = {
    val alphabet = new Alphabet()
    val labelAlphabet = new LabelAlphabet()
    val instances = (for (tweet <- tweetList) yield {
      tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          val featureSequence = new FeatureSequence(alphabet, features.length)
          for (feature <- features) {
            featureSequence.add(feature)
          }
          val featureVector = new FeatureVector(featureSequence)
          val label = labelAlphabet.lookupLabel(goldLabel)
//          val label = new FeatureVector(
//            labelAlphabet,
//            Array[Object]("label"), Array[Double](SentimentLabel.toDouble(goldLabel)))
          new Instance(featureVector, label, id, null)
      }
    }).toList

    val instanceList = new InstanceList(alphabet, labelAlphabet)
    for (instance <- instances) {
      instanceList.add(instance)
    }
    (alphabet, instanceList)
  }
  private val (alphabet, instanceList) = getInstanceList(tweets)
  private val trainer = new NaiveBayesTrainer()
  private val classifier = trainer.train(instanceList)

  def classify(tweet: GoldLabeledTweet) = {
    val GoldLabeledTweet(id, userid, features, goldLabel) = tweet
    val instance = {
      val featureSequence = new FeatureSequence(alphabet, features.length)
      for (feature <- features) {
        featureSequence.add(feature)
      }
      val featureVector = new FeatureVector(featureSequence)
      new Instance(featureVector, goldLabel, id, null)
    }
    val result = classifier.classify(instance)
    SystemLabeledTweet(id, userid, features, goldLabel,
      SentimentLabel.figureItOut(result.getLabeling.getBestLabel.getEntry.toString))
  }

}

