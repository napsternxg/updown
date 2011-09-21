package updown.app

import updown.data._
import updown.data.io._

import java.io._

import opennlp.maxent._
import opennlp.maxent.io._
import opennlp.model._

import org.clapper.argot._
import collection.mutable.HashMap

/**
 *
 * This object evaluates each tweet's system label against its gold label, and groups them according to target
 * (for the HCR dataset only), giving an accuracy score for each target.
 * 
 * @author Mike Speriosu
 */
object PerTargetEvaluator {

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage=Some("Updown"))

  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
  val targetsInputFile = parser.option[String](List("t", "targets"), "targets", "targets")

  val POS = "POS"
  val NEG = "NEG"
  val NEU = "NEU"

  //val DEFAULT_MIN_TPU = 1

  def apply(tweets: List[Tweet], targets: scala.collection.mutable.HashMap[String, String]) = evaluate(tweets, targets)

  def computeEvaluation(tweets: scala.List[Tweet], targets: HashMap[String, String]): (List[(String, Double)], Int, HashMap[String, List[Tweet]]) = {
    var totalError = 0.0
    var totalNumAbstained = 0
    //val usersToTweets = new scala.collection.mutable.HashMap[String, List[Tweet]] { override def default(s: String) = List() }
    val targetsToTweets = new scala.collection.mutable.HashMap[String, List[Tweet]] {
      override def default(s: String) = List()
    }
    var targetsToAccuracies = List[(String, Double)]()

    //val minTPU = DEFAULT_MIN_TPU
    //println(tweets.length)

    for (tweet <- tweets) {
      //val prevList = targetsToTweets(tweet.userid)
      if (targets.contains(tweet.id)){
        val curTarget = targets(tweet.id)
        targetsToTweets.put(curTarget, tweet :: targetsToTweets(curTarget))
      } else {
        System.err.println("missing target for "+tweet.id)
      }
    }

    //targetsToTweets.foreach(p => println(p._1+"   "+p._2.length))

    var numAbstained = 0
    for (target <- targetsToTweets.keys) {
      val curTweets = targetsToTweets(target)

      val abstained = curTweets.count(_.systemLabel == null)
      numAbstained += abstained
      val correct = curTweets.count(tweet => tweet.goldLabel == tweet.systemLabel) + abstained.toFloat / 2

      targetsToAccuracies = targetsToAccuracies ::: ((target, correct.toDouble / curTweets.length) :: Nil)
      /*for(tweet <- curTweets) {
        if(tweet.goldLabel == tweet.systemLabel)
      }*/
    }

    targetsToAccuracies.sortWith((x, y) => targetsToTweets(x._1).length >= targetsToTweets(y._1).length)
    (targetsToAccuracies, numAbstained, targetsToTweets)
  }

  def evaluate(tweets: List[Tweet], targets: scala.collection.mutable.HashMap[String, String]) = {
    val (targetsToAccuracies, numAbstained, targetsToTweets) = computeEvaluation(tweets, targets)

    println("\n***** PER TARGET EVAL *****")
    if(numAbstained > 0)
      println(numAbstained + " tweets were abstained on; assuming half (" + (numAbstained.toFloat/2) + ") were correct.")
    for((target, accuracy) <- targetsToAccuracies) {
      println(target+": "+accuracy+" ("+targetsToTweets(target).length+")")
    }
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
    if(targetsInputFile.value == None) {
      println("You must specify a targets input file via -t.")
      sys.exit(0)
    }

    val dataInputStream = new DataInputStream(new FileInputStream(modelInputFile.value.get));
    val reader = new BinaryGISModelReader(dataInputStream)

    val model = reader.getModel

    val goldLines = scala.io.Source.fromFile(goldInputFile.value.get,"utf-8").getLines.toList

    val tweets = TweetFeatureReader(goldInputFile.value.get)

    for(tweet <- tweets) {
      val result = model.eval(tweet.features.toArray)
      
      val posProb = result(0)

      val negProb = result(2)

      tweet.systemLabel = if(posProb >= negProb) POS else NEG
    }

    val targets = new scala.collection.mutable.HashMap[String, String]

    scala.io.Source.fromFile(targetsInputFile.value.get).getLines.foreach(p => targets.put(p.split("\t")(0).trim, p.split("\t")(1).trim))

    //targets.foreach(p => println(p._1+" "+p._2))

    evaluate(tweets, targets)
  }
}
