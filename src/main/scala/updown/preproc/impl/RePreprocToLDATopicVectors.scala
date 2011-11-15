package updown.preproc.impl

import java.io.File
import updown.preproc.GenericPreprocessor
import updown.data.io.TweetFeatureReader
import updown.data.{GoldLabeledTweet, SentimentLabel}
import updown.util.{TopicModel, LDATopicModel}
import org.clapper.argot.ArgotConverters._

/**
 * This preprocessor is suitable for any directory that contains files which should each be mapped to one instance
 * whose polarity is signified by the label given to the directory in the inputOption
 */
object RePreprocToLDATopicVectors extends GenericPreprocessor {
  var iterations = 1000
  var alpha = 30
  var beta = 0.1
  var numTopics = 3

  val iterationOption = parser.option[Int](List("iterations"), "INT", "the number of iterations for the training the topicModel")
  val alphaOption = parser.option[Int](List("alpha"), "INT", "the symmetric alpha hyperparameter for LDA")
  val betaOption = parser.option[Double](List("beta"), "DOUBLE", "the symmetric beta hyperparameter for LDA")
  val numTopicsOption = parser.option[Int](List("numTopics"), "INT", "the number of topics for LDA")

  def getInstanceIterator(fileName: String, polarity: String): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {
    if (iterationOption.value.isDefined) {
      iterations = iterationOption.value.get
    }
    if (alphaOption.value.isDefined) {
      alpha = alphaOption.value.get
    }
    if (betaOption.value.isDefined) {
      beta = betaOption.value.get
    }
    if (numTopicsOption.value.isDefined) {
      numTopics = numTopicsOption.value.get
    }

    // Thanks to a bug in Mallet, we have to cap alphaSum
    val alphaSum = 300 min (alpha * numTopics)
    val tweets = TweetFeatureReader(fileName)
    val model: TopicModel = new LDATopicModel(tweets, numTopics, iterations, alphaSum, beta)

    try {
      (for (tweet <- tweets) yield
        (tweet.id,
          tweet.userid,
          Left(tweet.goldLabel),
          model.inferTopics(tweet).mkString(" ")
          )
        ).iterator
    } catch {
      case e: MatchError =>
        logger.error("Couldn't figure out what sentiment '%s' is supposed to be." +
          " Try using 'pos', 'neg', or 'neu'. Skipping %s...".format(polarity, fileName))
        Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)]()
    }
  }
}