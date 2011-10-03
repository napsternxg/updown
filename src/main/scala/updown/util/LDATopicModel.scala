package updown.util

import cc.mallet.topics.ParallelTopicModel
import cc.mallet.types._
import scala.collection.JavaConversions._
import updown.data.{SentimentLabel, GoldLabeledTweet}

class LDATopicModel(tweets: List[GoldLabeledTweet], numTopics: Int, numIterations: Int, alphaSum: Double, beta: Double) extends TopicModel {
  private final val MAX_THREADS = 20

  private val (alphabet, instanceList) = getInstanceList(tweets)
  private val model = new ParallelTopicModel(numTopics, alphaSum, beta)
  model.addInstances(instanceList)
  model.setNumThreads(numTopics max MAX_THREADS)
  model.setNumIterations(numIterations)
  model.estimate()

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

  def getTopicPriors: List[Double] = {
    val result: Array[Double] = new Array[Double](numTopics)
    var sum = 0.0
    for (topicAssignment <- model.getData) {
      val temp: Array[Double] = model.getTopicProbabilities(topicAssignment.topicSequence)
      for (i <- 0 until result.length) {
        result(i) += temp(i)
        sum += temp(i)
      }
    }
    result.toList.map((double: Double) => double / sum)
  }

  def getTopicsPerInstance = {
    (for (topicAssignment <- model.getData) yield {
      val source = topicAssignment.instance.getName.toString
      val dist = model.getTopicProbabilities(topicAssignment.topicSequence)
      (source, dist.toList)
    }).toList
  }

  def getTopicsPerTarget = {
    val result = scala.collection.mutable.Map[SentimentLabel.Type,List[Double]]()
    for (topicAssignment <- model.getData) {
      val target = topicAssignment.instance.getTarget.asInstanceOf[SentimentLabel.Type]
      result(target) = result.getOrElse(target, (new Array[Double](numTopics)).toList).zip(model.getTopicProbabilities(topicAssignment.topicSequence).toList).map((pair) => pair._1+pair._2)
    }
    (for ((key, value) <- result) yield {
      val sum = value.reduce( _ + _ )
      (key->value.map(_ / sum))
    }).toMap
  }

  def inferTopics(tweet: GoldLabeledTweet): List[Double] = {
    val instance = tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          val featureSequence = new FeatureSequence(alphabet, features.length)
          for (feature <- features) {
            featureSequence.add(feature)
          }
          new Instance(featureSequence, goldLabel, id, null)
      }
    model.getInferencer.getSampledDistribution(instance, numIterations, 1, 1).toList
  }
}

