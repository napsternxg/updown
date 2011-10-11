package updown.app

import org.clapper.argot._

import updown.lex._
import updown.data._
import updown.data.io._

/**
 *
 * This object classifies tweets according to whether they have more positive or negative words in the MPQA
 * sentiment lexicon.
 *
 * @author Mike Speriosu
 */

object LexicalRatioClassifier {

  val POS = "POS"
  val NEG = "NEG"
  val NEU = "NEU"

  import ArgotConverters._

  val parser = new ArgotParser("updown run updown.app.JuntoClassifier", preUsage = Some("Updown"))

  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
  val mpqaInputFile = parser.option[String](List("p", "mpqa"), "mpqa", "MPQA sentiment lexicon input file")
  val targetsInputFile = parser.option[String](List("t", "targets"), "targets", "targets")

  def classifyTweet(numPosWords: Int, numNegWords: Int, numNeuWords: Int): SentimentLabel.Type = {
      if (numPosWords == numNegWords && numNeuWords == 0) null //this happens a lot...and more than 1/3 are either POS or NEG so accuracy would actually be improved by a random assignment of either NEG or POS
      else if (numPosWords == numNegWords && numNeuWords == numPosWords) SentimentLabel.Neutral //  could reasonably abstain on this
      else if (numPosWords == numNegWords && numNeuWords != 0) SentimentLabel.Neutral //Could reasonably abstain on this
      else if (numNeuWords > numPosWords && numNeuWords > numNegWords) SentimentLabel.Neutral
      else if (numPosWords > numNegWords && numPosWords > numNeuWords) SentimentLabel.Positive
      else if (numNegWords > numPosWords && numNegWords > numNeuWords) SentimentLabel.Negative
      else null
  }

  def classifyTweets(tweets: scala.List[Tweet], lexicon: MPQALexicon):List[SystemLabeledTweet] = {
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

  def main(args: Array[String]) {
    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(0)
    }

    if (goldInputFile.value == None) {
      println("You must specify a gold labeled input file via -g.")
      sys.exit(0)
    }
    if (mpqaInputFile.value == None) {
      println("You must specify an MPQA sentiment lexicon file via -p.")
      sys.exit(0)
    }


    val lexicon = MPQALexicon(mpqaInputFile.value.get)

    //println("mpqa lex val for word 'good': " + lexicon.peek("good"))    

    var totTweets = 0
    var numAbstained = 0

    val tweets = classifyTweets(TweetFeatureReader(goldInputFile.value.get), lexicon)
    
    PerTweetEvaluator(tweets)
    PerUserEvaluator(tweets)

    if (targetsInputFile.value != None) {
//      val targets = new scala.collection.mutable.HashMap[String, String]
//      scala.io.Source.fromFile(targetsInputFile.value.get, "utf-8").getLines.foreach(p => targets.put(p.split("|")(0).trim, p.split("|")(1).trim))
      val targets: Map[String, String] =
        (for (line <- scala.io.Source.fromFile(targetsInputFile.value.get, "UTF-8").getLines) yield {
          val arr = line.split("\\|")
          (arr(0)->arr(1))
        }).toMap
      PerTargetEvaluator(tweets, targets)
    }
  }
}