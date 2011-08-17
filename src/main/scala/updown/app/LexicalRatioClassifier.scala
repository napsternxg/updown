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
  val parser = new ArgotParser("updown run updown.app.JuntoClassifier", preUsage=Some("Updown"))
  
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
  val mpqaInputFile = parser.option[String](List("p", "mpqa"), "mpqa", "MPQA sentiment lexicon input file")
  val targetsInputFile = parser.option[String](List("t", "targets"), "targets", "targets")

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

    println("mpqa lex val for good: " + lexicon.peek("good"))    

    var totTweets = 0
    var numAbstained = 0

    for(tweet <- tweets){

      var numPosWords = 0
      var numNegWords = 0
      var numNeuWords = 0
      
      for(feature <- tweet.features) {
      
	if(lexicon.contains(feature)) {
          val entry = lexicon(feature)
          if(entry.isPositive) numPosWords += 1
          if(entry.isNegative) numNegWords += 1
	  if(entry.isNeutral) numNeuWords += 1
        }

      }
    
      
//      println(numPosWords + "\t" + numNeuWords + "\t" + numNegWords)
      if (numPosWords == numNegWords && numNeuWords == 0) numAbstained += 1   //this happens a lot...and more than 1/3 are either POS or NEG so accuracy would actually be improved by a random assignment
      else if(numPosWords == numNegWords && numNeuWords == numPosWords) tweet.systemLabel = "0"   // numAbstained += 1 could reasonably count as NEU, given relative low # of neutral entries
      else if (numPosWords == numNegWords && numNeuWords != 0) tweet.systemLabel = "0"   //Could reasonably abstain on this
      else if (numNeuWords > numPosWords && numNeuWords > numNegWords) tweet.systemLabel = "0"
      else if (numPosWords > numNegWords && numPosWords > numNeuWords) tweet.systemLabel = "1"
      else if (numNegWords  > numPosWords && numNegWords > numNeuWords) tweet.systemLabel = "-1"
//      else println("whoops")
//      println("tot abstained: " + numAbstained + " out of " + totTweets + " i.e. " + numAbstained.toFloat/totTweets.toFloat)

      totTweets +=1
    }
    PerTweetEvaluator(tweets)
    PerUserEvaluator(tweets)
    if(targetsInputFile.value != None) {
      val targets = new scala.collection.mutable.HashMap[String, String]

      scala.io.Source.fromFile(targetsInputFile.value.get, "utf-8").getLines.foreach(p => targets.put(p.split("\t")(0).trim, p.split("\t")(1).trim))
      PerTargetEvaluator(tweets, targets)
    }
  }
}


