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

  import ArgotConverters._
  val parser = new ArgotParser("updown run updown.app.JuntoClassifier", preUsage=Some("Updown"))
  
  val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val mpqaInputFile = parser.option[String](List("p", "mpqa"), "mpqa", "MPQA sentiment lexicon input file")
  val followerGraphFile = parser.option[String](List("f", "follower-graph"), "follower-graph", "twitter follower graph input file")
  val refCorpusProbsFile = parser.option[String](List("r", "reference-corpus-probabilities"), "reference-corpus-probabilities", "reference corpus probabilities input file")

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

    val tweets = TweetFeatureReader(goldInputFile.value.get)

    if(refCorpusProbsFile.value != None) {
      refCorpusNgramProbs = loadRefCorpusNgramProbs(refCorpusProbsFile.value.get)
      thisCorpusNgramProbs = computeNgramProbs(tweets)
    }

    val graph = createGraph(tweets, followerGraphFile.value.get, modelInputFile.value.get, mpqaInputFile.value.get)

    //graph.WriteToFileWithAlphabet("input-graph")
    graph.SaveEstimatedScores("input-graph")

    JuntoRunner(graph, mu1.value.getOrElse(DEFAULT_MU1), .01, .01, iterations.value.getOrElse(DEFAULT_ITERATIONS), false)

    graph.SaveEstimatedScores("output-graph")

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

    for(tweet <- tweets) {
      if(tweetIdsToPredictedLabels.containsKey(tweet.id)) {
        tweet.systemLabel = tweetIdsToPredictedLabels(tweet.id)
        //println(TWEET_ + tweet.id + "\t" + tweet.systemLabel)
      }
    }

    PerTweetEvaluator.evaluate(tweets)
  }
  
  def createGraph(tweets: List[Tweet], followerGraphFile: String, modelInputFile: String, mpqaInputFile: String) = {
    val edges = getTweetNgramEdges(tweets) ::: getFollowerEdges(followerGraphFile) ::: getUserTweetEdges(tweets)
    val seeds = getMaxentSeeds(tweets, modelInputFile) ::: getMPQASeeds(MPQALexicon(mpqaInputFile))/* ::: getEmoticonSeeds*/
    GraphBuilder(edges, seeds)
  }

  def getTweetNgramEdges(tweets: List[Tweet]): List[Edge] = {
    (for(tweet <- tweets) yield {
      for(ngram <- tweet.features) yield {
        //println(TWEET_ + tweet.id + "   " + NGRAM_ + ngram + "   " + getNgramWeight(ngram))
        val weight = getNgramWeight(ngram)
        if(weight > 0.0)
          new Edge(TWEET_ + tweet.id, NGRAM_ + ngram, weight)
        else
          null
      }
    }).flatten.filterNot(_ == null)
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
      if(tokens.length < 2)
        null
      else {
        //println(USER_ + tokens(0) + "   " + USER_ + tokens(1))
        new Edge(USER_ + tokens(0), USER_ + tokens(1), 1.0)
      }
    }).filterNot(_ == null).toList
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
    var wordCount = 0
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
