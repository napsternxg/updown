package updown.util

import cc.mallet.types._

import updown.data.GoldLabeledTweet
import java.io.File
import scala.Predef._
import scala._
import com.weiglewilczek.slf4s.Logging
import scala.collection.JavaConversions._

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

  def dumpState() {
    model.printState(new File("dmr.state"))
    model.writeParameters(new File("dmr.parameters"))
  }

  def getTopics: List[Topic] = {
    val topicPriors = List()// TODO FIXME model.getParameterValues
/* TODO FIXME   (for ((priorMap, i) <- topicPriors.zipWithIndex) yield {
      val wordDistributionMap = model.getSortedTopicWords(i)
        .filter(idSorter => idSorter.getWeight > 0)
        .map(idSorter => (alphabet.lookupObject(idSorter.getID).toString, idSorter.getWeight))
        .toMap
      Topic(priorMap.toMap.map{case(s,d)=>(s.toString,d.asInstanceOf[Double])}, wordDistributionMap)
    }).toList*/
    Nil
  }

  def getTopicPriors = {
    Array()// TODO FIXME model.getParameterValues.map(parameterMap=>parameterMap.get("label").asInstanceOf[Double]).toArray
  }

  def getIdsToTopicDist = {
    (for ((tweet, index) <- tweets.zipWithIndex) yield {
      (tweet.id, getTopicVector(model.getData.get(index).topicSequence.asInstanceOf[LabelSequence]))
    }).toMap
  }

  def getLabelsToTopicDists = {
    (for ((label, indexList: List[Int]) <- _labelToIndices) yield {
      (label, indexList.map {
        (i) => getTopicVector(model.getData.get(i).topicSequence.asInstanceOf[LabelSequence])
      })
    }).toMap
  }

  def inferTopics(tweet: GoldLabeledTweet): Array[Double] = {
    tweet match {
      case GoldLabeledTweet(id, userid, features, goldLabel) =>
        val featureSequence = new FeatureSequence(alphabet, features.length)
        for (feature <- features) {
          featureSequence.add(feature)
        }
        Array()// TODO FIXME getTopicVector(model.inferTopics(featureSequence,1000))
    }
//    Array[Double]()
  }

  def getTopicVector(topics: LabelSequence): Array[Double] = {
    val topicVector: Array[Double] = Array.ofDim[Double](numTopics)
    var total = 0.0
    val topicsIterator = topics.iterator()
    while (topicsIterator.hasNext) {
      val label: Label = topicsIterator.next().asInstanceOf[Label]
      topicVector(label.getIndex) += 1.0
      total += 1.0
    }
    val result:Array[Double] = topicVector.map(d => d / total)
    logger.trace("getting topic vector: %s".format(result.toList.toString))
    result
  }

  def save(filename: String) {
    model.printState(new File(filename))
  }
}

