package updown.app.experiment.lexical

import org.clapper.argot._

import updown.lex._
import updown.data._
import updown.data.io._
import updown.app.experiment.StaticExperiment
import updown.util.Statistics
import org.clapper.argot.ArgotConverters._

/**
 *
 * This object classifies tweets according to whether they have more positive, negative, or neutral
 * words in the MPQA sentiment lexicon.
 *
 * @author Mike Speriosu
 */

object LexicalRatioExperiment extends StaticExperiment {

  val mpqaInputFile = parser.option[String](List("p", "mpqa"), "mpqa", "MPQA sentiment lexicon input file")

  def classifyTweet(numPosWords: Int, numNegWords: Int, numNeuWords: Int): SentimentLabel.Type = {
    if (numPosWords == numNegWords && numNeuWords == 0) null //this happens a lot...and more than 1/3 are either POS or NEG so accuracy would actually be improved by a random assignment of either NEG or POS
    else if (numPosWords == numNegWords && numNeuWords == numPosWords) SentimentLabel.Neutral //  could reasonably abstain on this
    else if (numPosWords == numNegWords && numNeuWords != 0) SentimentLabel.Neutral //Could reasonably abstain on this
    else if (numNeuWords > numPosWords && numNeuWords > numNegWords) SentimentLabel.Neutral
    else if (numPosWords > numNegWords && numPosWords > numNeuWords) SentimentLabel.Positive
    else if (numNegWords > numPosWords && numNegWords > numNeuWords) SentimentLabel.Negative
    else null
  }

  def classifyTweets(tweets: scala.List[Tweet], lexicon: MPQALexicon): List[SystemLabeledTweet] = {
    (for (GoldLabeledTweet(id, userid, features, goldLabel) <- tweets) yield {

      var numPosWords = 0
      var numNegWords = 0
      var numNeuWords = 0

      for (feature <- features) {
        if (lexicon.contains(feature)) {
          val entry = lexicon(feature)
          if (entry.isPositive) numPosWords += 1
          if (entry.isNegative) numNegWords += 1
          if (entry.isNeutral) numNeuWords += 1
        }
      }
      SystemLabeledTweet(id, userid, features, goldLabel, classifyTweet(numPosWords, numNegWords, numNeuWords))
    }).toList
  }

  def doExperiment(dataSet: List[GoldLabeledTweet]) = {
    val mpqaFileName =
      mpqaInputFile.value match {
        case Some(filename: String) => filename
        case None =>
          parser.usage("You must specify an MPQA sentiment lexicon file via -p.")
      }
    val lexicon = MPQALexicon(mpqaFileName)
    val tweets = classifyTweets(dataSet, lexicon)
    tweets
  }

  def after(): Int = 0

}
