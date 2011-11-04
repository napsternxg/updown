package updown.app.experiment.labelprop

import opennlp.maxent.io.BinaryGISModelReader
import updown.lex.MPQALexicon
import java.io.{FileInputStream, DataInputStream}
import upenn.junto.app.JuntoRunner
import updown.app.experiment.{SplitExperiment, StaticExperiment}

import org.clapper.argot.ArgotConverters._
import upenn.junto.config.GraphBuilder._
import upenn.junto.config.{Label, GraphBuilder}
import updown.data.{SentimentLabel, SystemLabeledTweet, GoldLabeledTweet}

object SplitJuntoExperiment extends SplitExperiment with JuntoExperiment {

  import org.clapper.argot.ArgotConverters._

  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val mpqaInputFile = parser.option[String](List("p", "mpqa"), "mpqa", "MPQA sentiment lexicon input file")
  val followerGraphFile = parser.option[String](List("f", "follower-graph"), "follower-graph", "twitter follower graph input file")
  val followerGraphFileTest = parser.option[String](List("h", "follower-graph-test"), "follower-graph-test", "twitter follower graph input (TEST)")
  val refCorpusProbsFile = parser.option[String](List("r", "reference-corpus-probabilities"), "ref-corp-probs", "reference corpus probabilities input file")

  val edgeSeedSetOption = parser.option[String](List("e", "edge-seed-set-selection"), "edge-seed-set-selection", "edge/seed set selection")
  val topNOutputFile = parser.option[String](List("z", "top-n-file"), "top-n-file", "top-n-file")

  val mu1 = parser.option[Double](List("u", "mu1"), "mu1", "seed injection probability")
  val iterations = parser.option[Int](List("n", "iterations"), "iterations", "number of iterations")

  def createTransductiveGraph(trainTweets: List[GoldLabeledTweet], followerGraphFile: String, testTweets: List[GoldLabeledTweet], followerGraphFileTest: String, edgeSeedSet: String, getNgramWeight: (String) => Double) = {
    val totalTweets = trainTweets ::: testTweets
    val edges = (if (edgeSeedSet.contains("n")) getTweetNgramEdges(totalTweets, getNgramWeight) else Nil) :::
      (if (edgeSeedSet.contains("f")) (getFollowerEdges(followerGraphFile) ::: getUserTweetEdges(totalTweets) :::
        getFollowerEdges(followerGraphFileTest))
      else Nil)
    val seeds = getGoldSeeds(trainTweets)
    GraphBuilder(edges, seeds)
  }
  def getGoldSeeds(tweets: List[GoldLabeledTweet]): List[Label] = {
    for (tweet <- tweets) yield {
      tweet match {
        case GoldLabeledTweet(id, _, _, SentimentLabel.Positive) => new Label(TWEET_ + id, POS, 1.0)
        case GoldLabeledTweet(id, _, _, SentimentLabel.Negative) => new Label(TWEET_ + id, POS, 1.0)
        case GoldLabeledTweet(id, _, _, SentimentLabel.Neutral) => new Label(TWEET_ + id, POS, 1.0)
      }
    }
  }

  def doExperiment(trainTweets: List[GoldLabeledTweet], testTweets: List[GoldLabeledTweet]) = {
    logger.info("performing Junto experiment")
    logger.debug("loading model")
    val model =
      modelInputFile.value match {
        case Some(filename) =>
          new BinaryGISModelReader(new DataInputStream(new FileInputStream(modelInputFile.value.get))).getModel
        case None =>
          parser.usage("You must specify a model input file")
      }

    val lexicon =
      mpqaInputFile.value match {
        case Some(filename: String) =>
          MPQALexicon(filename)
        case None =>
          parser.usage("You must specify a lexicon file.")
      }



    val edgeSeedSet = edgeSeedSetOption.value.getOrElse(DEFAULT_EDGE_SEED_SET)

    val getNgramWeight = getNgramWeightFn(refCorpusProbsFile.value, testTweets)

    val graph =
      (followerGraphFile.value,followerGraphFileTest.value) match {
        case (Some(filename: String),Some(filenameTest: String)) =>
          createTransductiveGraph(trainTweets, filename, testTweets, filenameTest, edgeSeedSet, getNgramWeight)
      }

    logger.debug("running label prop")
    JuntoRunner(graph, mu1.value.getOrElse(DEFAULT_MU1), .01, .01, iterations.value.getOrElse(DEFAULT_ITERATIONS), false)


    val res: List[SystemLabeledTweet] = evaluateGraphResults(testTweets, graph, lexicon, getNgramWeight, topNOutputFile.value)
    res
  }
}