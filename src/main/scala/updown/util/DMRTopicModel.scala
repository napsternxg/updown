package updown.util

import cc.mallet.types._

import updown.data.GoldLabeledTweet
import java.io.File
import scala.Predef._
import scala._
import com.weiglewilczek.slf4s.Logging

class DMRTopicModel(tweets: List[GoldLabeledTweet], numTopics: Int, numIterations: Int
                    , alphaSum: Double, beta: Double) extends TopicModel with Logging {
  private val (_alphabet, instanceList) = getInstanceList(tweets)
  _alphabet.stopGrowth()
  val alphabet = _alphabet
  logger.debug("creating dmr topic model with %d topics".format(numTopics))
  private var model = new cc.mallet.topics.DMRTopicModel(numTopics)
  model.setOptimizeInterval(100)
  model.setTopicDisplay(100, 10)
  model.addInstances(instanceList)
  model.setNumIterations(numIterations)
  model.estimate()

  //  ParallelTopicModel.logger.setLevel(Level.OFF)
  private val _labelToIndices = tweets.zipWithIndex.groupBy {
    case (tweet, index) => tweet.goldLabel
  }.map {
    case (label, tweetList) => (label, tweetList.map {
      case (tweet, index) => index
    })
  }
  /*
  override def toString(): String = {
    model.printTopWords()
    model.printTopWords(20, true)
  }*/

  def dumpToStdOut = {
    model.printTopWords(System.out,20,true)
    System.out.flush()
  }

  def getTopics: List[Topic] = {
    List[Topic]()
  }

  def getTopicPriors = {
    Array[Double](numTopics)
  }

  def getIdsToTopicDist = {
    Map[String, Array[Double]]()
  }

  def getLabelsToTopicDists = {
    (for ((label, indexList: List[Int]) <- _labelToIndices) yield {
      (label, indexList.map {
        (i) => getTopicVector(model.getData.get(i).topicSequence.asInstanceOf[LabelSequence])
      })
    }).toMap
  }

  def inferTopics(tweet: GoldLabeledTweet): Array[Double] = {
    /*tweet match {
      case GoldLabeledTweet(id, userid, features, goldLabel) =>
        val featureSequence = new FeatureSequence(alphabet, features.length)
        for (feature <- features) {
          featureSequence.add(feature)
        }
        getTopicVector(model.topicInferenceLast(featureSequence, numIterations))
    }*/
    Array[Double]()
  }

  def getTopicVector(topics: LabelSequence): Array[Double] = {
    val topicVector: Array[Double] = Array.ofDim[Double](numTopics)
    var total = 0.0
    val topicsIterator = topics.iterator()
    while (topicsIterator.hasNext) {
      val label:Label= topicsIterator.next().asInstanceOf[Label]
      topicVector(label.getIndex) += 1.0
      total += 1.0
    }
    topicVector.map(d => d / total)
  }

  def save(filename: String) {
    model.printState(new File(filename))
  }
}

