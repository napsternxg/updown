package updown.util

import cc.mallet.topics.ParallelTopicModel
import cc.mallet.types._
import scala.collection.JavaConversions._
import updown.data.{SentimentLabel, GoldLabeledTweet}
import java.io.File

class LDATopicModelFromFile(fileName: String) extends TopicModel {

  private var model = ParallelTopicModel.read(new File(fileName))
  private val alphabet = model.alphabet
  private val numTopics = model.numTopics
  private val numIterations = model.numIterations

  def getTopics: List[Topic] = {
    val priors = getTopicPriors
    val topicsToAlphaIds = scala.collection.mutable.Map[Int,List[(Int,Double)]]()

    val wordsTopicsCounts = (for ((topicCounts, typeIndex) <- model.typeTopicCounts.zipWithIndex) yield {
      val word = alphabet.lookupObject(typeIndex).toString
      (for (topicCount <- topicCounts) yield {
        val topic = topicCount & model.topicMask
        val count = topicCount >> model.topicBits
        (word,topic,count)
      }).iterator
    }).iterator.flatten.toList


    val res = (for (i <- 0 until numTopics) yield {
      val wordCounts = wordsTopicsCounts.filter((triple)=>(triple._2==i && triple._3!=0))
      val sum = wordCounts.map((triple)=>triple._3).reduce(_ + _)
      Topic(priors(i), wordCounts.map((triple)=>(triple._1->(triple._3.toDouble/sum))).toMap)
    }).toList

    res
  }

  def getTopicPriors = {
    val result: Array[Double] = new Array[Double](numTopics)
    var sum = 0.0
    for (topicAssignment <- model.getData) {
      val temp: Array[Double] = model.getTopicProbabilities(topicAssignment.topicSequence)
      for (i <- 0 until result.length) {
        result(i) += temp(i)
        sum += temp(i)
      }
    }
    result.toList.map((double: Double) => double / sum).toArray
  }

  def getIdsToTopicDist = {
    (for (topicAssignment <- model.getData) yield {
      val source = topicAssignment.instance.getName.toString
      val dist = model.getTopicProbabilities(topicAssignment.topicSequence)
      (source, dist)
    }).toMap
  }

    def getLabelsToTopicDists = {
    val result = scala.collection.mutable.Map[SentimentLabel.Type,List[Array[Double]]]().withDefaultValue(Nil)
    for (topicAssignment <- model.getData) {
      val label = topicAssignment.instance.getTarget.asInstanceOf[SentimentLabel.Type]
      result(label) = model.getTopicProbabilities(topicAssignment.topicSequence) :: result(label)
    }
    result.toMap // immutize
  }

  def inferTopics(tweet: GoldLabeledTweet) = {
    val instance = tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          val featureSequence = new FeatureSequence(alphabet, features.length)
          for (feature <- features) {
            featureSequence.add(feature)
          }
          new Instance(featureSequence, goldLabel, id, null)
      }
    model.getInferencer.getSampledDistribution(instance, numIterations, 1, 1)
  }

  def save(filename: String) {
    model.write(new File(filename))
  }
}

