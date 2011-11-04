package updown.app.experiment.labelprop

import opennlp.maxent.io.BinaryGISModelReader
import org.clapper.argot.ArgotParser._
import updown.lex.MPQALexicon
import opennlp.model.AbstractModel
import upenn.junto.config.{Edge, Label, GraphBuilder}
import updown.data.{ProbabilityLexicon, SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import java.util.zip.GZIPInputStream
import java.io.{ObjectInputStream, FileInputStream, DataInputStream}
import upenn.junto.app.JuntoRunner
import scala.collection.JavaConversions._
import updown.app.experiment.{Experiment, StaticExperiment}
import upenn.junto.graph.Graph
import com.weiglewilczek.slf4s.Logging

trait JuntoExperiment extends Logging {
  val DEFAULT_MU1 = .005
  val DEFAULT_ITERATIONS = 100
  val DEFAULT_EDGE_SEED_SET = "nfmoe"
  val nodeRE = """^(.+_)(.+)$""".r
  val posEmoticons = """:) :D =D =) :] =] :-) :-D :-] ;) ;D ;] ;-) ;-D ;-]""".split(" ")
  val negEmoticons = """:( =( :[ =[ :-( :-[ :’( :’[ D:""".split(" ")

  val TWEET_ = "tweet_"
  val USER_ = "user_"
  val NGRAM_ = "ngram_"
  val POS = "POS"
  val NEG = "NEG"
  val NEU = "NEU"

  // for weighting MPQA seeds
  val BIG = 0.9
  val BIG_COMP = .1
  val SMALL = 0.8
  val SMALL_COMP = .2


  val getNgramWeightFn: (Any, List[GoldLabeledTweet]) => ((String) => Double) =
    (refCorpusFileOption, trainSet) => {
      refCorpusFileOption match {
        case Some(filename: String) =>
          val refCorpusNgramProbs = loadRefCorpusNgramProbs(filename)
          val thisCorpusNgramProbs = computeNgramProbs(trainSet)
          (ngram) => {
            val numerator = thisCorpusNgramProbs(ngram)
            val denominator = refCorpusNgramProbs.getNgramProb(ngram)

            if (denominator == 0.0) 0.0 //ngram not found in reference corpus; assume NOT relevant to this corpus
            else if (numerator > denominator) math.log(numerator / denominator)
            else 0.0
          }

        case None => (str) => 1.0
      }
    }

  def getTweetNgramEdges(tweets: List[GoldLabeledTweet], getNgramWeight: (String) => Double): List[Edge] = {
    (for (tweet <- tweets) yield {
      for (ngram <- tweet.features) yield {
        val weight = getNgramWeight(ngram)
        if (weight > 0.0) Some(new Edge(TWEET_ + tweet.id, NGRAM_ + ngram, weight)) else None
      }
    }).flatten.flatten
  }

  def getFollowerEdges(followerGraphFile: String): List[Edge] = {
    (for (line <- scala.io.Source.fromFile(followerGraphFile, "utf-8").getLines) yield {
      val tokens = line.split("\t")
      if (tokens.length < 2 || tokens(0).length == 0 || tokens(1).length == 0) None else Some(new Edge(USER_ + tokens(0), USER_ + tokens(1), 1.0))
    }).flatten.toList
  }

  def getUserTweetEdges(tweets: List[GoldLabeledTweet]): List[Edge] = (for (tweet <- tweets) yield new Edge(USER_ + tweet.userid, TWEET_ + tweet.id, 1.0))

  def getMaxentSeeds(tweets: List[GoldLabeledTweet], model: AbstractModel): List[Label] = {
    val labels = model.getDataStructures()(2).asInstanceOf[Array[String]]
    val posIndex = labels.indexOf("1")
    val negIndex = labels.indexOf("-1")
    val neuIndex = labels.indexOf("0")

    var nNeut = 0
    val res =
    (for (tweet <- tweets) yield {
      val result = model.eval(tweet.features.toArray)
      val posProb = if (posIndex >= 0) result(posIndex) else 0.0
      val negProb = if (negIndex >= 0) result(negIndex) else 0.0
      val neuProb = if (neuIndex >= 0) result(neuIndex) else 0.0
      if (neuIndex > 0)
        nNeut += 1

      new Label(TWEET_ + tweet.id, POS, posProb) :: new Label(TWEET_ + tweet.id, NEG, negProb) :: new Label(TWEET_ + tweet.id, NEU, neuProb) :: Nil
    }).flatten
    println("neutrals:%d".format(nNeut))
    res
  }

  def getMPQASeeds(lexicon: MPQALexicon): List[Label] = {
    (for (word <- lexicon.keySet.toList) yield {
      val entry = lexicon(word)
      val posWeight =
        if (entry.isStrong && entry.isPositive) BIG
        else if (entry.isWeak && entry.isPositive) SMALL
        else if (entry.isStrong && entry.isNegative) BIG_COMP
        else /*if(entry.isWeak && entry.isNegative)*/ SMALL_COMP

      val negWeight =
        if (entry.isStrong && entry.isPositive) BIG_COMP
        else if (entry.isWeak && entry.isPositive) SMALL_COMP
        else if (entry.isStrong && entry.isNegative) BIG
        else /*if(entry.isWeak && entry.isNegative)*/ SMALL

      val neuWeight = 0.5 //Matt has little to no inkling of what is appropriate here.


      new Label(NGRAM_ + word, POS, posWeight) :: new Label(NGRAM_ + word, NEG, negWeight) :: new Label(NGRAM_ + word, NEU, neuWeight) :: Nil
    }).flatten
  }

  def getEmoticonSeeds(): List[Label] = {
    (for (emo <- posEmoticons) yield {
      new Label(NGRAM_ + emo, POS, BIG) ::
        new Label(NGRAM_ + emo, NEG, BIG_COMP) :: Nil
    }).toList.flatten :::
      (for (emo <- negEmoticons) yield {
        new Label(NGRAM_ + emo, NEG, BIG) ::
          new Label(NGRAM_ + emo, POS, BIG_COMP) :: Nil
      }).toList.flatten/* :::
      (for (emo <- negEmoticons) yield {
        new Label(NGRAM_ + emo, NEG, BIG) ::
          new Label(NGRAM_ + emo, POS, BIG_COMP) :: Nil
      }).toList.flatten*/
  }

  def createGraph(tweets: List[GoldLabeledTweet], followerGraphFile: String, model: AbstractModel, lexicon: MPQALexicon, edgeSeedSet: String, getNgramWeight: (String) => Double) = {
    val edges = (if (edgeSeedSet.contains("n")) getTweetNgramEdges(tweets, getNgramWeight) else Nil) :::
      (if (edgeSeedSet.contains("f")) (getFollowerEdges(followerGraphFile) ::: getUserTweetEdges(tweets)) else Nil)
    val seeds = (if (edgeSeedSet.contains("m")) getMaxentSeeds(tweets, model) else Nil) :::
      (if (edgeSeedSet.contains("o")) getMPQASeeds(lexicon) else Nil) :::
      (if (edgeSeedSet.contains("e")) getEmoticonSeeds else Nil)
    GraphBuilder(edges, seeds)
  }

  def loadRefCorpusNgramProbs(filename: String): ProbabilityLexicon /*scala.collection.mutable.HashMap[String, Double]*/ = {
    val refProbs = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filename))).readObject()

    refProbs match {
      case refProbLex: ProbabilityLexicon => refProbLex
      case _ => throw new ClassCastException
    }
  }

  def getWordCount(tweets: List[GoldLabeledTweet]): Int = {
    (for (tweet <- tweets) yield {
      (for (feature <- tweet.features) yield {
        1
      }).sum
    }).sum
  }

  def computeNgramProbs(tweets: List[GoldLabeledTweet]): scala.collection.mutable.HashMap[String, Double] = {
    val probs = new scala.collection.mutable.HashMap[String, Double] {
      override def default(s: String) = 0.0
    }
    for (tweet <- tweets) {
      for (feature <- tweet.features) {
        probs.put(feature, probs(feature) + 1.0)
      }
    }

    probs.foreach(p => probs.put(p._1, p._2 / getWordCount(tweets)))

    probs
  }

  def evaluateGraphResults(tweets: scala.List[GoldLabeledTweet], graph: Graph, lexicon: MPQALexicon, getNgramWeight: (String) => Double, topNOutputFileOption: Option[String]): List[SystemLabeledTweet] = {
    val tweetIdsToPredictedLabels = new scala.collection.mutable.HashMap[String, SentimentLabel.Type]



    logger.debug("testing model")
    val ngramsToPositivity = new scala.collection.mutable.HashMap[String, Double]
    val ngramsToNegativity = new scala.collection.mutable.HashMap[String, Double]
    val ngramsToNeutrality = new scala.collection.mutable.HashMap[String, Double]

    val thisCorpusNgramProbs = computeNgramProbs(tweets)

    for ((id, vertex) <- graph._vertices) {
      val nodeRE(nodeType, nodeName) = id
      if (nodeType == TWEET_) {
        val predictions = vertex.GetEstimatedLabelScores
        val posProb = predictions.get(POS)
        val negProb = predictions.get(NEG)
        val neuProb = predictions.get(NEU)
        val maxProb = math.max(posProb, math.max(negProb, neuProb))

        tweetIdsToPredictedLabels(nodeName) =
          if (neuProb == maxProb)
            SentimentLabel.Neutral
          else if (posProb == maxProb)
            SentimentLabel.Positive
          else
            SentimentLabel.Negative
      }
      else if (topNOutputFileOption != None && nodeType == NGRAM_ && !lexicon.contains(nodeName)
        && getNgramWeight(nodeName) >= 1.0 && thisCorpusNgramProbs(nodeName) * getWordCount(tweets) >= 5.0) {
        val predictions = vertex.GetEstimatedLabelScores
        val posProb = predictions.get(POS)
        val negProb = predictions.get(NEG)
        val neuProb = predictions.get(NEU)

        ngramsToPositivity.put(nodeName, posProb)
        ngramsToNegativity.put(nodeName, negProb)
        ngramsToNeutrality.put(nodeName, neuProb)

      }
    }
    logger.debug("predicted nPos:%d nNeg:%d nNeu:%d".format(
      tweetIdsToPredictedLabels.count(i => i._2 == SentimentLabel.Positive),
      tweetIdsToPredictedLabels.count(i => i._2 == SentimentLabel.Negative),
      tweetIdsToPredictedLabels.count(i => i._2 == SentimentLabel.Neutral)
    ))
    val res = for (tweet <- tweets) yield {

      tweet match {
        case GoldLabeledTweet(id, userid, features, goldLabel) =>
          SystemLabeledTweet(id, userid, features, goldLabel,
            if (tweetIdsToPredictedLabels.contains(id)) {
              tweetIdsToPredictedLabels(id)
            } else {
              SentimentLabel.Abstained
            })
      }
    }
    res
  }



  def after(): Int = 0
}