package updown.app.experiment.labelprop

import opennlp.maxent.io.BinaryGISModelReader
import org.clapper.argot.ArgotParser
import org.clapper.argot.ArgotParser._
import org.clapper.argot.ArgotConverters._
import updown.lex.MPQALexicon._
import updown.lex.MPQALexicon
import upenn.junto.config.GraphBuilder._
import opennlp.model.AbstractModel
import upenn.junto.config.{Edge, Label, GraphBuilder}
import updown.data.{ProbabilityLexicon, SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import java.util.zip.GZIPInputStream
import java.io.{ObjectInputStream, FileInputStream, DataInputStream}
import updown.data.io.TweetFeatureReader._
import updown.app.experiment.{SplitExperiment, StaticExperiment}
import upenn.junto.app.JuntoRunner._
import upenn.junto.app.JuntoRunner
import scala.collection.JavaConversions._

object StaticJuntoExperiment extends StaticExperiment with JuntoExperiment{
  val modelInputFile = parser.option[String](List("m", "model"), "model", "model input")
  val mpqaInputFile = parser.option[String](List("p", "mpqa"), "mpqa", "MPQA sentiment lexicon input file")
  val followerGraphFile = parser.option[String](List("f", "follower-graph"), "follower-graph", "twitter follower graph input file")
  val refCorpusProbsFile = parser.option[String](List("r", "reference-corpus-probabilities"), "ref-corp-probs", "reference corpus probabilities input file")

  val edgeSeedSetOption = parser.option[String](List("e", "edge-seed-set-selection"), "edge-seed-set-selection", "edge/seed set selection")
  val topNOutputFile = parser.option[String](List("z", "top-n-file"), "top-n-file", "top-n-file")

  val mu1 = parser.option[Double](List("u", "mu1"), "mu1", "seed injection probability")
  val iterations = parser.option[Int](List("n", "iterations"), "iterations", "number of iterations")


  def doExperiment(tweets: List[GoldLabeledTweet]) = {
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

    val getNgramWeight = getNgramWeightFn(refCorpusProbsFile.value, tweets)

    val graph =
      followerGraphFile.value match {
        case Some(filename: String) =>
          createGraph(tweets, filename, model, lexicon, edgeSeedSet, getNgramWeight)
      }

    logger.debug("running label prop")
    JuntoRunner(graph, mu1.value.getOrElse(DEFAULT_MU1), .01, .01, iterations.value.getOrElse(DEFAULT_ITERATIONS), false)


    val res: List[SystemLabeledTweet] = evaluateGraphResults(tweets, graph, lexicon, getNgramWeight, topNOutputFile.value)
    res
  }
}