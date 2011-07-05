package updown.app

import updown.data._
import updown.data.io._
import updown.lex._

import java.io._
import java.util.zip._

import opennlp.maxent._
import opennlp.maxent.io._
import opennlp.model._

import upenn.junto.app._
import upenn.junto.config._

import org.clapper.argot._

import scala.collection.JavaConversions._

/**
 *
 * @author Mike Speriosu
 */
object JuntoClassifier {

  val DEFAULT_MU1 = .005
  val DEFAULT_ITERATIONS = 100
  val DEFAULT_EDGE_SEED_SET = "nfmoe"

  val TOP_N = 20

  // for weighting MPQA seeds
  val BIG = 0.9
  val BIG_COMP = .1
  val SMALL = 0.8
  val SMALL_COMP = .2

  val TWEET_ = "tweet_"
  val USER_ = "user_"
  val NGRAM_ = "ngram_"
  val POS = "POS"
  val NEG = "NEG"

  val posEmoticons = """:) :D =D =) :] =] :-) :-D :-] ;) ;D ;] ;-) ;-D ;-]""".split(" ")
  val negEmoticons = """:( =( :[ =[ :-( :-[ :’( :’[ D:""".split(" ")

  val nodeRE = """^(.+_)(.+)$""".r

  var refCorpusNgramProbs:scala.collection.mutable.HashMap[String, Double] = null
  var thisCorpusNgramProbs:scala.collection.mutable.HashMap[String, Double] = null

  var wordCount = 0

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.JuntoClassifier", preUsage=Some("Updown"))
  
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val mpqaInputFile = parser.option[String](List("p", "mpqa"), "mpqa", "MPQA sentiment lexicon input file")
  val followerGraphFile = parser.option[String](List("f", "follower-graph"), "follower-graph", "twitter follower graph input file")
  val refCorpusProbsFile = parser.option[String](List("r", "reference-corpus-probabilities"), "ref-corp-probs", "reference corpus probabilities input file")

  val edgeSeedSetOption = parser.option[String](List("e", "edge-seed-set-selection"), "edge-seed-set-selection", "edge/seed set selection")
  val targetsInputFile = parser.option[String](List("t", "targets"), "targets", "targets")
  val topNOutputFile = parser.option[String](List("z", "top-n-file"), "top-n-file", "top-n-file")

  val mu1 = parser.option[Double](List("u", "mu1"), "mu1", "seed injection probability")
  val iterations = parser.option[Int](List("n", "iterations"), "iterations", "number of iterations")

  def main(args: Array[String]) {
    try { parser.parse(args) }
    catch { case e: ArgotUsageException => println(e.message); sys.exit(0)}

    if(modelInputFile.value == None) {
      println("You must specify a model input file via -m.")
      sys.exit(0)
    }
    if(goldInputFile.value == None) {
      println("You must specify a gold labeled input file via -g.")
      sys.exit(0)
    }
    if(mpqaInputFile.value == None) {
      println("You must specify an MPQA sentiment lexicon file via -p.")
      sys.exit(0)
    }
    if(followerGraphFile.value == None) {
      println("You must specify a follower graph file via -f.")
      sys.exit(0)
    }

    val edgeSeedSet = edgeSeedSetOption.value.getOrElse(DEFAULT_EDGE_SEED_SET)

    val tweets = TweetFeatureReader(goldInputFile.value.get)

    if(refCorpusProbsFile.value != None) {
      refCorpusNgramProbs = loadRefCorpusNgramProbs(refCorpusProbsFile.value.get)
      thisCorpusNgramProbs = computeNgramProbs(tweets)
    }

    val lexicon = MPQALexicon(mpqaInputFile.value.get)

    val graph = createGraph(tweets, followerGraphFile.value.get, modelInputFile.value.get, lexicon, edgeSeedSet)

    //graph.SaveEstimatedScores("input-graph")

    JuntoRunner(graph, mu1.value.getOrElse(DEFAULT_MU1), .01, .01, iterations.value.getOrElse(DEFAULT_ITERATIONS), false)

    //graph.SaveEstimatedScores("output-graph")

    val tweetIdsToPredictedLabels = new scala.collection.mutable.HashMap[String, String]

    val ngramsToPositivity = new scala.collection.mutable.HashMap[String, Double]
    val ngramsToNegativity = new scala.collection.mutable.HashMap[String, Double]

    for ((id, vertex) <- graph._vertices) {
      val nodeRE(nodeType, nodeName) = id
      if(nodeType == TWEET_) {
        val predictions = vertex.GetEstimatedLabelScores
        val posProb = predictions.get(POS)
        val negProb = predictions.get(NEG)

        if(posProb >= negProb)
          tweetIdsToPredictedLabels.put(nodeName, POS)
        else
          tweetIdsToPredictedLabels.put(nodeName, NEG)
      }
      else if(topNOutputFile.value != None && nodeType == NGRAM_ && !lexicon.contains(nodeName)
              && getNgramWeight(nodeName) >= 1.0 && thisCorpusNgramProbs(nodeName)*wordCount >= 5.0) {
        val predictions = vertex.GetEstimatedLabelScores
        val posProb = predictions.get(POS)
        val negProb = predictions.get(NEG)

        ngramsToPositivity.put(nodeName, posProb)
        ngramsToNegativity.put(nodeName, negProb)
      }
    }

    for(tweet <- tweets) {
      if(tweetIdsToPredictedLabels.containsKey(tweet.id)) {
        tweet.systemLabel = tweetIdsToPredictedLabels(tweet.id)
        //println(TWEET_ + tweet.id + "\t" + tweet.systemLabel)
      }
    }

    PerTweetEvaluator(tweets)
    PerUserEvaluator(tweets)
    if(targetsInputFile.value != None) {
      val targets = new scala.collection.mutable.HashMap[String, String]

      scala.io.Source.fromFile(targetsInputFile.value.get).getLines.foreach(p => targets.put(p.split("\t")(0).trim, p.split("\t")(1).trim))
      PerTargetEvaluator(tweets, targets)
    }

    if(topNOutputFile.value != None) {
      val tnout = new BufferedWriter(new FileWriter(topNOutputFile.value.get))
      //val topNPos = ngramsToPositivity.toList/*.filterNot(p => lexicon.contains(p._1))*/.sortWith((x, y) => x._2 >= y._2).slice(0, TOP_N)
      //val topNNeg = ngramsToPositivity.toList.sortWith((x, y) => x._2 <= y._2).slice(0, TOP_N)//ngramsToNegativity.toList/*.filterNot(p => lexicon.contains(p._1))*/.sortWith((x, y) => x._2 >= y._2).slice(0, TOP_N)

      val ngramsToRatios = ngramsToPositivity.toList.map(p => (p._1, p._2 / ngramsToNegativity(p._1)))

      //topNPos.foreach(p => tnout.write(p._1+" "+p._2+"\n"))
      //tnout.write("\n\n\n")
      //topNNeg.foreach(p => tnout.write(p._1+" "+p._2+"\n"))
      val mostPos = ngramsToRatios.sortWith((x, y) => x._2 >= y._2).slice(0, TOP_N)
      mostPos.foreach(p => tnout.write(p._1+"\t"+p._2+"\n"))
      mostPos.foreach(p => tnout.write(p._1+", "))
      tnout.write("\n\n\n\n")
      val mostNeg = ngramsToRatios.sortWith((x, y) => x._2 <= y._2).slice(0, TOP_N)
      mostNeg.foreach(p => tnout.write(p._1+"\t"+p._2+"\n"))
      mostNeg.foreach(p => tnout.write(p._1+", "))
      tnout.write("\n")

      tnout.close
    }
  }
  
  def createGraph(tweets: List[Tweet], followerGraphFile: String, modelInputFile: String, lexicon: MPQALexicon, edgeSeedSet: String) = {
    val edges = (if(edgeSeedSet.contains("n")) getTweetNgramEdges(tweets) else Nil) :::
                (if(edgeSeedSet.contains("f")) (getFollowerEdges(followerGraphFile) ::: getUserTweetEdges(tweets)) else Nil)
    val seeds = (if(edgeSeedSet.contains("m")) getMaxentSeeds(tweets, modelInputFile) else Nil) :::
                (if(edgeSeedSet.contains("o")) getMPQASeeds(lexicon) else Nil) :::
                (if(edgeSeedSet.contains("e")) getEmoticonSeeds else Nil)
    GraphBuilder(edges, seeds)
  }

  def getTweetNgramEdges(tweets: List[Tweet]): List[Edge] = {
    (for(tweet <- tweets) yield {
      for(ngram <- tweet.features) yield {
        val weight = getNgramWeight(ngram)
        //println(TWEET_ + tweet.id + "   " + NGRAM_ + ngram + "   " + weight)
        if(weight > 0.0) {
          //if(ngram == "mccain") println("mccain: " + weight)
          Some(new Edge(TWEET_ + tweet.id, NGRAM_ + ngram, weight))
        }
        else
          None
      }
    }).flatten.flatten
  }

  def getUserTweetEdges(tweets: List[Tweet]): List[Edge] = {
    for(tweet <- tweets) yield {
      //println(USER_ + tweet.userid + "   " + TWEET_ + tweet.id)
      new Edge(USER_ + tweet.userid, TWEET_ + tweet.id, 1.0)
    }
  }

  def getFollowerEdges(followerGraphFile: String): List[Edge] = {
    (for(line <- scala.io.Source.fromFile(followerGraphFile).getLines) yield {
      val tokens = line.split("\t")
      if(tokens.length < 2 || tokens(0).length == 0 || tokens(1).length == 0)
        None
      else {
        //println(USER_ + tokens(0) + "   " + USER_ + tokens(1))
        Some(new Edge(USER_ + tokens(0), USER_ + tokens(1), 1.0))
      }
    }).flatten.toList
  }

  def getMaxentSeeds(tweets: List[Tweet], modelInputFile: String): List[Label] = {
    val dataInputStream = new DataInputStream(new FileInputStream(modelInputFile));
    val reader = new BinaryGISModelReader(dataInputStream)
    val model = reader.getModel

    (for(tweet <- tweets) yield {
      val result = model.eval(tweet.features.toArray)
      val posProb = result(0)
      val negProb = result(1)

      //println(TWEET_ + tweet.id + "   " + POS + "   " + posProb)
      //println(TWEET_ + tweet.id + "   " + NEG + "   " + negProb)
      new Label(TWEET_ + tweet.id, POS, posProb) :: new Label(TWEET_ + tweet.id, NEG, negProb) :: Nil
    }).flatten
  }

  def getMPQASeeds(lexicon: MPQALexicon): List[Label] = {
    (for(word <- lexicon.keySet.toList) yield {
      val entry = lexicon(word)
      val posWeight =
        if(entry.isStrong && entry.isPositive) BIG
        else if(entry.isWeak && entry.isPositive) SMALL
        else if(entry.isStrong && entry.isNegative) BIG_COMP
        else /*if(entry.isWeak && entry.isNegative)*/ SMALL_COMP

      val negWeight =
        if(entry.isStrong && entry.isPositive) BIG_COMP
        else if(entry.isWeak && entry.isPositive) SMALL_COMP
        else if(entry.isStrong && entry.isNegative) BIG
        else /*if(entry.isWeak && entry.isNegative)*/ SMALL
      
      new Label(NGRAM_ + word, POS, posWeight) :: new Label(NGRAM_ + word, NEG, negWeight) :: Nil
    }).flatten
  }

  def getEmoticonSeeds(): List[Label] = {
    (for(emo <- posEmoticons) yield { new Label(NGRAM_ + emo, POS, BIG) ::
                                     new Label(NGRAM_ + emo, NEG, BIG_COMP) :: Nil}).toList.flatten :::
    (for(emo <- negEmoticons) yield { new Label(NGRAM_ + emo, NEG, BIG) ::
                                     new Label(NGRAM_ + emo, POS, BIG_COMP) :: Nil}).toList.flatten
  }

  def loadRefCorpusNgramProbs(filename: String): scala.collection.mutable.HashMap[String, Double] = {
    val gis = new GZIPInputStream(new FileInputStream(filename))
    val ois = new ObjectInputStream(gis)
    val refProbs = ois.readObject()

    refProbs match {
      case refProbsHM: scala.collection.mutable.HashMap[String, Double] => refProbsHM
      case _ => throw new ClassCastException
    }
  }

  def computeNgramProbs(tweets: List[Tweet]): scala.collection.mutable.HashMap[String, Double] = {
    val probs = new scala.collection.mutable.HashMap[String, Double] { override def default(s: String) = 0.0 }
    /*var */wordCount = 0
    for(tweet <- tweets) {
      for(feature <- tweet.features) {
        probs.put(feature, probs(feature) + 1.0)
        wordCount += 1
      }
    }

    probs.foreach(p => probs.put(p._1, p._2 / wordCount))

    probs
  }

  def getNgramWeight(ngram: String): Double = {
    if(refCorpusNgramProbs == null || thisCorpusNgramProbs == null)
      return 1.0
    else {
      val numerator = thisCorpusNgramProbs(ngram)
      val denominator = refCorpusNgramProbs(ngram)

      if(denominator == 0.0) //ngram not found in reference corpus; assume NOT relevant to this corpus
        return 0.0
      else if(numerator > denominator) {
        //println(ngram + "   this: " + numerator + "   ref: " + denominator + "   weight: " + math.log(numerator / denominator))
        return math.log(numerator / denominator)
      }
      else
        return 0.0
    }
  }
}

object TransductiveJuntoClassifier {

  import JuntoClassifier._

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.TransductiveJuntoClassifier", preUsage=Some("Updown"))
  
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold training labeled input")
  val testInputFile = parser.option[String](List("v", "test"), "test", "gold test labeled input")
  //val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  //val mpqaInputFile = parser.option[String](List("p", "mpqa"), "mpqa", "MPQA sentiment lexicon input file")
  val followerGraphFile = parser.option[String](List("f", "follower-graph"), "follower-graph", "twitter follower graph input file (TRAIN)")
  val followerGraphFileTest = parser.option[String](List("h", "follower-graph-test"), "follower-graph-test", "twitter follower graph input (TEST)")
  val refCorpusProbsFile = parser.option[String](List("r", "reference-corpus-probabilities"), "ref-corp-probs", "reference corpus probabilities input file")

  val edgeSeedSetOption = parser.option[String](List("e", "edge-seed-set-selection"), "edge-seed-set-selection", "edge/seed set selection")
  val targetsInputFile = parser.option[String](List("t", "targets"), "targets", "targets")

  val mu1 = parser.option[Double](List("u", "mu1"), "mu1", "seed injection probability")
  val iterations = parser.option[Int](List("n", "iterations"), "iterations", "number of iterations")
  
  def main(args: Array[String]) = {

    try { parser.parse(args) }
    catch { case e: ArgotUsageException => println(e.message); sys.exit(0)}

    val edgeSeedSet = edgeSeedSetOption.value.getOrElse(DEFAULT_EDGE_SEED_SET)

    val trainTweets = TweetFeatureReader(goldInputFile.value.get)
    val testTweets = TweetFeatureReader(testInputFile.value.get)
    val totalTweets = trainTweets ::: testTweets

    //val testTweetIds = testTweets.map(_.id).toSet

    if(refCorpusProbsFile.value != None) {
      refCorpusNgramProbs = loadRefCorpusNgramProbs(refCorpusProbsFile.value.get)
      thisCorpusNgramProbs = computeNgramProbs(totalTweets)
    }

    val graph = createTransductiveGraph(trainTweets, followerGraphFile.value.get, testTweets, followerGraphFileTest.value.get, edgeSeedSet)

    JuntoRunner(graph, mu1.value.getOrElse(DEFAULT_MU1), .01, .01, iterations.value.getOrElse(DEFAULT_ITERATIONS), false)

    val tweetIdsToPredictedLabels = new scala.collection.mutable.HashMap[String, String]

    for ((id, vertex) <- graph._vertices) {
      val nodeRE(nodeType, nodeName) = id
      if(nodeType == TWEET_) {
        val predictions = vertex.GetEstimatedLabelScores
        val posProb = predictions.get(POS)
        val negProb = predictions.get(NEG)

        if(posProb >= negProb)
          tweetIdsToPredictedLabels.put(nodeName, POS)
        else
          tweetIdsToPredictedLabels.put(nodeName, NEG)
      }
    }

    for(tweet <- testTweets) {
      if(tweetIdsToPredictedLabels.containsKey(tweet.id)) {
        tweet.systemLabel = tweetIdsToPredictedLabels(tweet.id)
        //println(TWEET_ + tweet.id + "\t" + tweet.systemLabel)
      }
    }

    PerTweetEvaluator.evaluate(testTweets)
    PerUserEvaluator.evaluate(testTweets)
    if(targetsInputFile.value != None) {
      val targets = new scala.collection.mutable.HashMap[String, String]

      scala.io.Source.fromFile(targetsInputFile.value.get).getLines.foreach(p => targets.put(p.split("\t")(0).trim, p.split("\t")(1).trim))
      PerTargetEvaluator(testTweets, targets)
    }
  }

  def createTransductiveGraph(trainTweets: List[Tweet], followerGraphFile: String, testTweets: List[Tweet],  followerGraphFileTest: String, edgeSeedSet: String) = {
    val totalTweets = trainTweets ::: testTweets
    val edges = (if(edgeSeedSet.contains("n")) getTweetNgramEdges(totalTweets) else Nil) :::
                (if(edgeSeedSet.contains("f")) (getFollowerEdges(followerGraphFile) ::: getUserTweetEdges(totalTweets) :::
                                                getFollowerEdges(followerGraphFileTest)) else Nil)
    val seeds = getGoldSeeds(trainTweets)
    /*val seeds = (if(edgeSeedSet.contains("m")) getMaxentSeeds(tweets, modelInputFile) else Nil) :::
                (if(edgeSeedSet.contains("o")) getMPQASeeds(MPQALexicon(mpqaInputFile)) else Nil) :::
                (if(edgeSeedSet.contains("e")) getEmoticonSeeds else Nil)*/
    //edges.filter(_.weight <= 0.5).foreach(println)
    GraphBuilder(edges, seeds)
  }

  def getGoldSeeds(tweets: List[Tweet]): List[Label] = {
    for(tweet <- tweets) yield {
      if(tweet.goldLabel == POS)
        new Label(TWEET_ + tweet.id, POS, 1.0)
      else
        new Label(TWEET_ + tweet.id, NEG, 1.0)
    }
  }
}
