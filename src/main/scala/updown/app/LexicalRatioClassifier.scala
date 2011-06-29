package updown.app

import org.clapper.argot._

import updown.lex._
import updown.data._
import updown.data.io._

object LexicalRatioClassifier {

  val POS = "POS"
  val NEG = "NEG"

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.JuntoClassifier", preUsage=Some("Updown"))
  
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
  val mpqaInputFile = parser.option[String](List("p", "mpqa"), "mpqa", "MPQA sentiment lexicon input file")

  def main(args: Array[String]) = {
    try { parser.parse(args) }
    catch { case e: ArgotUsageException => println(e.message); sys.exit(0)}

    if(goldInputFile.value == None) {
      println("You must specify a gold labeled input file via -g.")
      sys.exit(0)
    }
    if(mpqaInputFile.value == None) {
      println("You must specify an MPQA sentiment lexicon file via -p.")
      sys.exit(0)
    }

    val tweets = TweetFeatureReader(goldInputFile.value.get)

    val lexicon = MPQALexicon(mpqaInputFile.value.get)

    var numAbstained = 0
    for(tweet <- tweets) {

      var numPosWords = 0
      var numNegWords = 0
      for(feature <- tweet.features) {
        if(lexicon.contains(feature)) {
          val entry = lexicon(feature)
          if(entry.isPositive) numPosWords += 1
          if(entry.isNegative) numNegWords += 1
        }
      }

      if(numPosWords == numNegWords) numAbstained += 1
      else if(numPosWords > numNegWords) {
        tweet.systemLabel = POS
      }
      else {//if(numNegWords > numPosWords)
        tweet.systemLabel = NEG
      }
    }

    PerTweetEvaluator(tweets)
    PerUserEvaluator(tweets)
  }
}
