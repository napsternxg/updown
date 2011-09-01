package updown.app

import updown.data._
import updown.data.io._

import java.io._

import opennlp.maxent._
import opennlp.maxent.io._
import opennlp.model._

import org.clapper.argot._

/**
 *
 * This object evaluates each user's overall system positivity (fraction of tweets labeled positive by the system)
 * against his or her gold positivity (fraction of tweets truly positive), resulting in a mean squared error.
 * 
 * @author Mike Speriosu
 */
object PerUserEvaluator {

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage=Some("Updown"))

  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
  val doRandom = parser.option[String](List("r", "random"), "random", "do random")

  val POS = "POS"
  val NEG = "NEG"
  val NEU = "NEU"

  val DEFAULT_MIN_TPU = 3 

  def apply(tweets: List[Tweet]) = evaluate(tweets)

  def evaluate(tweets: List[Tweet]) = {
    var totalError = 0.0; var totalErrorAlt = 0.0
    var totalNumAbstained = 0
    val usersToTweets = new scala.collection.mutable.HashMap[String, List[Tweet]] { override def default(s: String) = List() }

    val minTPU = DEFAULT_MIN_TPU

    for(tweet <- tweets) usersToTweets.put(tweet.userid, usersToTweets(tweet.userid) ::: (tweet :: Nil))

    val usersToTweetsFiltered = usersToTweets.filter(p => p._2.length >= minTPU) 

    for(userid <- usersToTweetsFiltered.keys) {
      val curTweets = usersToTweetsFiltered(userid)

      var numAbstained = 0
      if(curTweets.length >= minTPU) {
        var numGoldPos = 0.0; var numSysPos = 0.0
	var numGoldNeg = 0.0; var numSysNeg = 0.0 
        var numGoldNeu = 0.0; var numSysNeu = 0.0

	for(tweet <- curTweets) {
          if (tweet.goldLabel == POS) numGoldPos += 1
	  else if (tweet.goldLabel == NEG) numGoldNeg += 1
	  else if (tweet.goldLabel == NEU) numGoldNeu += 1
          if (tweet.systemLabel == POS && doRandom.value == None) numSysPos += 1
	  else if (tweet.systemLabel == NEG && doRandom.value == None) numSysNeg += 1
	  else if (tweet.systemLabel == NEU && doRandom.value == None) numSysNeu += 1
	  else if(tweet.systemLabel == null || doRandom.value != None) numAbstained += 1
        }

        numSysPos += numAbstained.toFloat / 3
        /*if(doRandom.value != None) {
          numSysPos = numGoldPos / 2
          numAbstained = 0
        }*/
        totalError += math.pow(((numGoldPos+numGoldNeg+numGoldNeu) - (numSysPos+numSysNeg+numSysNeu)) / curTweets.length, 2)
	totalErrorAlt += math.pow(((numGoldPos) - (numSysPos)) / curTweets.length, 2)
        totalNumAbstained += numAbstained
      }
    }

    totalError /= usersToTweetsFiltered.size
    totalErrorAlt /= usersToTweetsFiltered.size

    println("\n***** PER USER EVAL *****")

    if(totalNumAbstained > 0){
      //println(totalNumAbstained + " tweets were abstained on; assuming half (" + (totalNumAbstained.toFloat/2) + ") were positive.")
      println(totalNumAbstained + " tweets were abstained on; assuming one-third (" + (totalNumAbstained.toFloat/3) + ") were positive.")
      
    }

    println("Number of users evaluated: " + usersToTweetsFiltered.size + " (min of " + minTPU + " tweets per user)")
    println("Mean squared error: " + totalError)
    //println("Mean squared error for alternate way of calculatin' it which may or may not come out to be the same number: " + totalErrorAlt)
  }

  def main(args: Array[String]) {
    try { parser.parse(args) }
    catch { case e: ArgotUsageException => println(e.message); sys.exit(0) }

    if(modelInputFile.value == None) {
      println("You must specify a model input file via -m.")
      sys.exit(0)
    }
    if(goldInputFile.value == None) {
      println("You must specify a gold labeled input file via -g.")
      sys.exit(0)
    }

    val dataInputStream = new DataInputStream(new FileInputStream(modelInputFile.value.get));
    val reader = new BinaryGISModelReader(dataInputStream)

    val model = reader.getModel
    
    /* Caution! Confusing code reuse below! */
    val labels = model.getDataStructures()(2).asInstanceOf[Array[String]]
    val posIndex = labels.indexOf("1")
    val negIndex = labels.indexOf("-1")
    val neuIndex = labels.indexOf("0 ")

    val goldLines = scala.io.Source.fromFile(goldInputFile.value.get, "utf-8").getLines.toList

    val tweets = TweetFeatureReader(goldInputFile.value.get)

    for(tweet <- tweets) {
      val result = model.eval(tweet.features.toArray)
      
      val posProb = result(posIndex)//result(0)
      val negProb = result(negIndex)//result(2)
      val neuProb = result(neuIndex)//result(1)
      if(posProb >= negProb && posProb >= neuProb) tweet.systemLabel = POS
      else if (negProb > posProb && negProb > neuProb) tweet.systemLabel = NEG
      else if (neuProb > posProb && neuProb > negProb) tweet.systemLabel == NEU
    }

    evaluate(tweets)
  }
}
