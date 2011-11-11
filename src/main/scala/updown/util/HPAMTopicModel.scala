package updown.util

import cc.mallet.types._
import scala.collection.JavaConversions._

import updown.data.{SentimentLabel, GoldLabeledTweet}
import java.io.File
import cc.mallet.topics.HierarchicalPAM
import cc.mallet.util.Randoms
import scala.Predef._
import scala._
import com.weiglewilczek.slf4s.Logging

class HPAMTopicModel(tweets: List[GoldLabeledTweet], numSuperTopics: Int, numSubTopics: Int, numIterations: Int
                     /*, alphaSum: Double, beta: Double*/) extends TopicModel with Logging {
  private final val MAX_THREADS = 20

  private val (_alphabet, instanceList) = getInstanceList(tweets)
  _alphabet.stopGrowth()
  val alphabet = _alphabet
  logger.debug("creating pam topic model with %d supers and %d subs".format(numSuperTopics, numSubTopics))
  private var model = new HierarchicalPAM(numSuperTopics, numSubTopics, 1.0, 1.0)
  model.estimate(instanceList, instanceList, numIterations, 50, 10, 100, "", new Randoms())

  //  ParallelTopicModel.logger.setLevel(Level.OFF)
  private val _labelToIndices = tweets.zipWithIndex.groupBy {
    case (tweet, index) => tweet.goldLabel
  }.map {
    case (label, tweetList) => (label, tweetList.map {
      case (tweet, index) => index
    })
  }

  override def toString(): String = {
    model.printTopWords(20, true)
  }

  def getTopics: List[Topic] = {
    val supers = model.getSuperTopicPriorWeights
    val subs = model.getSuperSubTopicPriorWeights
    var result = Topic(1.0, Map(("TOPIC_1" -> supers(1)), ("TOPIC_2" -> supers(2)), ("TOPIC_3" -> supers(3)))) //root
    for (i <- 0 until numSuperTopics) {
      val sub = subs(i)
    }
    /*
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


        val res = (for (i <- 0 until numSubTopics) yield {
          val wordCounts = wordsTopicsCounts.filter((triple)=>(triple._2==i && triple._3!=0))
          val sum = wordCounts.map((triple)=>triple._3).reduce(_ + _)
          Topic(priors(i), wordCounts.map((triple)=>(triple._1->(triple._3.toDouble/sum))).toMap)
        }).toList

        res
    */
    List[Topic]()
  }

  /**
   * Since PAM makes a tree of topics, we just start with the supers and then append each of the children
   */
  def getTopicPriors = {
    val supers: Array[Double] = model.getSuperTopicPriorWeights
    val subs: Array[Array[Double]] = model.getSuperSubTopicPriorWeights
    var result = supers.toList
    for (i <- 0 until numSuperTopics) {
      result = result ::: subs(i).toList
    }
    result.toArray
  }

  def getIdsToTopicDist = {
    /*  (for (topicAssignment <- model.getData) yield {
        val source = topicAssignment.instance.getName.toString
        val dist = model.getTopicProbabilities(topicAssignment.topicSequence)
        (source, dist.toList)
      }).toList
    */
    Map[String, Array[Double]]()
  }

  def getLabelsToTopicDists = {
    (for ((label, indexList: List[Int]) <- _labelToIndices) yield {
      (label, indexList.map((i) => getTopicVector(model.getTopicsForDoc(i))))
    }).toMap
  }

  def computeDistribution(assignments: List[Int]): Map[Int, Double] = {
    val counts = scala.collection.mutable.Map[Int, Double]().withDefaultValue(0.0)
    for (t <- assignments) {
      counts(t) += 1
    }
    for (k <- counts.keys) {
      counts(k) /= assignments.length
    }
    counts.toMap
  }

  def inferTopics(tweet: GoldLabeledTweet): Array[Double] = {
    tweet match {
      case GoldLabeledTweet(id, userid, features, goldLabel) =>
        val featureSequence = new FeatureSequence(alphabet, features.length)
        for (feature <- features) {
          featureSequence.add(feature)
        }
        getTopicVector(model.destructiveTopicInference(featureSequence, numIterations/2))
    }
  }

  def getTopicVector(topics: Array[Array[Int]]): Array[Double] = {
    val superCounts = computeDistribution(topics(0).toList).withDefaultValue(0.0)
    val subCounts = computeDistribution(topics(1).toList).withDefaultValue(0.0)
    val result = Array.ofDim[Double](
      1 + numSuperTopics
//        + numSubTopics
    )
    for (i <- 0 until (1 + numSuperTopics)) {result(i) = superCounts(i)}
//    for (i <- 0 until (numSubTopics)) {result(1+numSuperTopics+i) = subCounts(i)}
    result
  }

  def save(filename: String) {
    model.printState(new File(filename))
  }
}

