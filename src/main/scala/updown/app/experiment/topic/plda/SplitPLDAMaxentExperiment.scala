package updown.app.experiment.topic.plda

import updown.app.experiment.SplitExperiment
import updown.app.experiment.topic.util.MaxentDiscriminant
import scalanlp.io._
import java.io.{OutputStreamWriter, FileOutputStream, File}
import scalanlp.pipes._
import scalanlp.stage._
import scalanlp.stage.text._
import scalanlp.text.tokenize._
import edu.stanford.nlp.tmt.stage._
import edu.stanford.nlp.tmt.model.DirichletParams._
import edu.stanford.nlp.tmt.model.llda.LabeledLDADataset
import edu.stanford.nlp.tmt.model.plda.{SharedKTopicsPerLabel, PLDAModelParams}
import scala.Predef._
import updown.data.SystemLabeledTweet._
import updown.data.{SystemLabeledTweet, SentimentLabel, GoldLabeledTweet}
import org.clapper.argot.ArgotParser
import org.clapper.argot.ArgotConverters._

trait CLOptions {
  val parser: ArgotParser
  val tmpDirOption = parser.option[File](List("tmp", "tmpDir"), "DIR", "The location to store temp files in") {
    (s, opt) =>
      val file = new File(s)
      if (file.isDirectory) {
        file
      } else if (!file.exists) {
        parser.usage("tmp dir does not exist")
      } else {
        parser.usage("Invaid tmp dir")
      }
  }
  val numIterationOption = parser.option[Int](List("iterations"), "INT", "the number of iterations for the training the topicModel")
  //  val alphaOption = parser.option[Double](List("alpha"), "INT", "the symmetric alpha hyperparameter for LDA")
  //  val betaOption = parser.option[Double](List("beta"), "DOUBLE", "the symmetric beta hyperparameter for LDA")
  val numTopicsPerLabelOption = parser.option[Int](List("numLabelTopics"), "INT", "the number of topics for PLDA")
  val numTopicsInBackgroundOption = parser.option[Int](List("numBgroundTopics"), "INT", "the number of topics for PLDA")
  val extraTopicTrainingSetOption = parser.multiOption[String]("T", "FILE",
    "extra inputs to be used to train the topic model, not the classifier.")

  def getNumBackgroundTopics = numTopicsInBackgroundOption.value match {
    case Some(value: Int) => value
    case _ => 1
  }

  def getNumTopicsPerLabel = numTopicsPerLabelOption.value match {
    case Some(value: Int) => SharedKTopicsPerLabel(value);
    // or could specify the number of topics per label based on the values
    // in a two-column CSV file containing label name and number of topics
    // val numTopicsPerLabel = CustomKTopicsPerLabel(
    //  CSVFile("topics-per-label.csv").read[Iterator[(String,Int)]].toMap);
    case _ => SharedKTopicsPerLabel(4);
    // or could specify the number of topics per label based on the values
    // in a two-column CSV file containing label name and number of topics
    // val numTopicsPerLabel = CustomKTopicsPerLabel(
    //  CSVFile("topics-per-label.csv").read[Iterator[(String,Int)]].toMap);

  }

  def getNumIterations = numIterationOption.value match {
    case Some(value: Int) => value
    case _ => 1000
  }

  def getTmpDir = tmpDirOption.value match {
    case Some(file: File) => file
    case _ =>
      val file = new File("tmp")
      if (!file.exists()) {
        file.mkdir()
      } else if (file.exists() && !file.isDirectory) {
        System.err.println("Could not create tmp dir in working dir. Try specifying the --tmp option.")
        System.exit(1)
      }
      file
  }
}

object SplitPLADMaxentExperiment extends SplitExperiment with MaxentDiscriminant with CLOptions {
  val rand = new java.util.Random()

  def createCSVIntermediate(set: List[GoldLabeledTweet], name: String): File = {
    val tmpFile = new File(getTmpDir.getAbsolutePath + java.io.File.separator + "splitPLDAMeExp_" + name + "_" + rand.nextLong())
    val os = new FileOutputStream(tmpFile)
    val out = new OutputStreamWriter(os, "utf8")
    for (GoldLabeledTweet(id, userid, features, goldLabel) <- set) {
      out.write("\"%s\",\"%s\",\"%s\",\"%s\"\n".format(
        id.toString,
        userid.toString,
        features.mkString(" "),
        if (name == "test") {
          "1 -1"
        } else {
          goldLabel.toString
        }))
    }
    out.flush()
    out.close()
    tmpFile
  }

  private val _tokenizer = {
    SimpleEnglishTokenizer() ~> // tokenize on space and punctuation
      MinimumLengthFilter(3) // take terms with >=3 characters
  }

  def getLLDADataset(infile: File, tokenizer: Tokenizer = _tokenizer): LabeledLDADataset[(String, scala.Iterable[String], scala.Iterable[String])] = {
    val trainSource = CSVFile(infile) ~> IDColumn(1)

    val text = {
      trainSource ~> // read from the source file
        Column(3) ~> // select column containing text
        TokenizeWith(tokenizer) ~> // tokenize with tokenizer above
        TermCounter() ~> // collect counts (needed below)
        TermMinimumDocumentCountFilter(4) ~> // filter terms in <4 docs
        TermDynamicStopListFilter(30) ~> // filter out 30 most common terms
        DocumentMinimumLengthFilter(5) // take only docs with >=5 terms
    }

    // define fields from the dataset we are going to slice against
    val labels = {
      trainSource ~> // read from the source file
        Column(4) ~> // take column two, the year
        TokenizeWith(WhitespaceTokenizer()) ~> // turns label field into an array
        TermCounter() ~> // collect label counts
        TermMinimumDocumentCountFilter(10) // filter labels in < 10 docs
    }

    val dataset = LabeledLDADataset(text, labels);
    dataset
  }

  def doExperiment(testSet: List[GoldLabeledTweet], trainSet: List[GoldLabeledTweet]) = {
    val trainFile = createCSVIntermediate(trainSet, "train")
    val trainDataset = getLLDADataset(trainFile)

    // define the model parameters
    val modelParams = PLDAModelParams(trainDataset,
      getNumBackgroundTopics, getNumTopicsPerLabel,
      termSmoothing = 0.01, topicSmoothing = 0.01);

    // Name of the output model folder to generate
    val modelPath = new File(getTmpDir.getAbsolutePath + File.separator + "plda-updown-" + trainDataset.signature + "-" + modelParams.signature);

    // Trains the model, writing to the given output path
    val model = TrainCVB0PLDA(modelParams, trainDataset, output = modelPath, maxIterations = getNumIterations);

    val modelTopicTermMatrix = (for (i <- 0 until model.numTopics) yield {
      model.getTopicTermDistribution(i)
    }).toArray
    val trainMap = trainSet.map(tweet => (tweet.id, tweet)).toMap

    val trainDistributions = (for (doc <- trainDataset.iterator) yield {
      val termTopics = (for (term <- doc.terms) yield {
        val probs = Array.ofDim[Double](model.numTopics)
        for (topic <- 0 until model.numTopics) {
          probs(topic) += modelTopicTermMatrix(topic)(term)
        }
        probs.indexOf(probs.max)
      })
      val topicProportions = Array.ofDim[Double](model.numTopics)
      for (termTopic <- termTopics) {
        topicProportions(termTopic) += 1.0
      }
      for (topic <- 0 until model.numTopics) {
        topicProportions(topic) /= doc.terms.length
      }
      (doc.id, topicProportions)

    }).toList
    val labelsToTopicDists = scala.collection.mutable.Map[SentimentLabel.Type, List[Array[Double]]]().withDefaultValue(Nil)
    for ((outId, dist) <- trainDistributions) yield {
      val GoldLabeledTweet(inId, inUID, _, inLabel) = trainMap(outId)
      assert(outId.equals(inId))
      labelsToTopicDists(inLabel) = dist :: labelsToTopicDists(inLabel)
    }
    val discriminantFn = getDiscriminantFn(labelsToTopicDists.toMap)
    val testFile = createCSVIntermediate(testSet, "test")
    val testDataset = getLLDADataset(testFile, model.tokenizer match {
      case Some(tokenizer) =>
        tokenizer
      case _ =>
        _tokenizer
    })
    val testMap = testSet.map(tweet => (tweet.id, tweet)).toMap
    (for (doc <- testDataset.iterator) yield {
      val termTopics = (for (term <- doc.terms) yield {
        val probs = Array.ofDim[Double](model.numTopics)
        for (topic <- 0 until model.numTopics) {
          probs(topic) += modelTopicTermMatrix(topic)(term)
        }
        probs.indexOf(probs.max)
      })
      val topicProportions = Array.ofDim[Double](model.numTopics)
      for (topic <- termTopics) {
        topicProportions(topic) += 1.0
      }
      for (topic <- 0 until model.numTopics) {
        topicProportions(topic) /= doc.terms.length
      }
      val GoldLabeledTweet(inId, inUID, inFeatures, inLabel) = testMap(doc.id)
      val (outLabel: String, outcomes: String) = discriminantFn(topicProportions.map(d => d.toFloat))
      logger.trace("labeling id:%s gold:%2s with label:%2s from outcomes:%s".format(
        inId,
        inLabel.toString,
        outLabel.toString,
        outcomes))
      SystemLabeledTweet(inId, inUID, inFeatures, inLabel, SentimentLabel.figureItOut(outLabel))
    }).toList
  }

  def after() = 0
}
/*
val (outLabel: String, outcomes: String) = discriminantFn(topicDist.map(d => d.toFloat))
      logger.trace("labeling id:%s gold:%2s with label:%2s from outcomes:%s".format(
        inId,
        inLabel.toString,
        outLabel.toString,
        outcomes))
      SystemLabeledTweet(inId, inUID, inFeatures, inLabel, SentimentLabel.figureItOut(outLabel))
 */
