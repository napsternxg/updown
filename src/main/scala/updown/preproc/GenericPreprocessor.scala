package updown.preproc

import org.clapper.argot.{ArgotUsageException, ArgotParser, ArgotConverters}
import updown.data.SentimentLabel
import updown.util.TokenizationPipes
import com.weiglewilczek.slf4s.Logging
import collection.immutable.List._
import java.io.{File, FileOutputStream, OutputStreamWriter}

abstract class GenericPreprocessor extends Logging {

  import ArgotConverters._

  var pipeStages: Map[String, (List[String]) => List[String]] =
    Map[String, (List[String]) => List[String]](
      ("lowerCase" -> TokenizationPipes.toLowercase),
      ("addBigrams" -> TokenizationPipes.addNGrams(2)),
      ("basicTokenize" -> TokenizationPipes.basicTokenize),
      ("twokenize" -> TokenizationPipes.twokenize),
      ("twokenizeSkipGtOneGrams" -> TokenizationPipes.twokenizeSkipGtOneGrams),
      ("filterAlpha") -> TokenizationPipes.filterOnRegex("\\p{Alpha}+"),
      ("filterAlphaQuote") -> TokenizationPipes.filterOnRegex("(\\p{Alpha}|')+"),
      ("splitSpace" -> TokenizationPipes.splitOnDelimiter(" ")),
      ("removeStopwords") -> {
        (s: List[String]) =>
          throw new Error("Not implemented. This should have been replaced in the main method.")
      }
    )
  val defaultPipeline = "twokenize|removeStopwords"
  val parser = new ArgotParser("updown run updown.preproc.PreprocStanfordTweets", preUsage = Some("Updown"))
  val inputFiles = parser.multiParameter[File]("input files", "input files", true) {
    (s, op) =>
      val f = new File(s)
      if (f.isFile) {
        f
      } else {
        parser.usage("input %s was not a file.".format(s))
      }
  }
  val stopListFile = parser.option[String](List("s", "stoplist"), "stoplist", "path to stoplist file")
  val startId = parser.option[Int](List("start-id"), "ID", "id at which to start numbering lines")
  val textPipeline = parser.option[String](List("textPipeline"), "PIPELINE",
    ("specify the desired pipe stages seperated by |: \"addBiGrams|twokenize\". " +
      "Available options are in %s.").format(pipeStages.keySet))

  val targetFile = parser.option[String](List("t", "target"), "target", "target file")
  val featureFile = parser.option[String](List("f", "feature"), "feature", "feature file")

  val vocabSize = parser.option[Int]("vocabSize", "SIZE", "The number of words to allow in the vocabulary.")

  def getInstanceIterator(file: File): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)]

  def getInputIterator(files: Seq[File]): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {
    logger.debug("entering getInputIterator")
    (for (file <- files) yield {
      getInstanceIterator(file)
    }).iterator.flatten
  }

  def runThroughPipeLine(text: String, pipeLine: List[(List[String]) => List[String]]): List[String] = {
    var res = List(text)
    for (pipeStage <- pipeLine) {
      res = pipeStage(res)
    }
    res
  }

  def writeInstance(id: String, reviewer: String, text: String, polarity: String, writer: OutputStreamWriter) {
    writer.write("%s|%s|%s|%s\n".format(id, reviewer, text, polarity))
  }

  def writeTarget(id: String, target: String, writer: OutputStreamWriter) {
    writer.write("%s|%s\n".format(id, target))
  }

  def getVocabulary(size: Int, pipeline: List[(List[String]) => List[String]], inputs: Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)]): Set[String] = {
    val counts = scala.collection.mutable.Map[String, Int]().withDefaultValue(0)
    for ((_, _, _, text) <- inputs) {
      val outputText = runThroughPipeLine(text, pipeline).map((s) => s.replaceAll(",", "-COMMA-").replaceAll("\\|", "-PIPE-"))
      for (token <- outputText) {
        counts(token) = counts(token) + 1
      }
    }
    counts.toList.sortBy(_._2).reverse.map {
      case (s, c) => s
    }.take(size).toSet
  }

  def before() {}

  def apply(args: Array[String]) {
    logger.debug(args.toList.toString)
    try {
      parser.parse(args)
      before()
      // SET UP IO


      val inputLines = getInputIterator(inputFiles.value)
      val targetWriter = new OutputStreamWriter(
        targetFile.value match {
          case Some(fileName) => new FileOutputStream(new File(fileName))
          case None => System.out
        }, "UTF-8")

      // Note: if you want to squelch output entirely, you can initialize the writer with
      // new java.io.OutputStream() { public void write ( int b ) { } }

      val featureWriter = new OutputStreamWriter(
        featureFile.value match {
          case Some(fileName) => new FileOutputStream(new File(fileName))
          case None => System.out
        }, "UTF-8")

      val stopSet: Set[String] =
        stopListFile.value match {
          case Some(fileName) =>
            scala.io.Source.fromFile(fileName).getLines.toSet
          case None => Set("a", "the", ".")
        }
      val tokpipe: (String, List[String] => List[String]) = ("removeStopwords", TokenizationPipes.filterOnStopset(stopSet))
      pipeStages = pipeStages + tokpipe


      logger.debug("Pipeline option: %s".format(textPipeline.value))
      val pipeline: List[(List[String]) => List[String]] = {
        val arg: String =
          if (textPipeline.value.isDefined) {
            textPipeline.value.get
          } else {
            defaultPipeline
          }
        (for (pipeStage <- arg.split("\\|")) yield {
          if (pipeStages.keySet.contains(pipeStage)) {
            pipeStages(pipeStage)
          } else {
            parser.usage("invalid pipeStage: %s".format(pipeStage))
          }
        }).toList
      }
      logger.debug("Pipeline: %s".format(pipeline))

      // STATS
      val idNumStart =
        startId.value match {
          case Some(id) => id
          case None => 0
        }
      var numLines = 0
      var numSkipped = 0
      var numClasses = scala.collection.mutable.Map[SentimentLabel.Type, Int]().withDefaultValue(0)
      var numLabels = 0
      var numTokens = 0
      // RUN
      val vocab = vocabSize.value match {
        case Some(i: Int) => getVocabulary(i, pipeline, getInputIterator(inputFiles.value))
        case _ => Set[String]()
      }
      for ((id, reviewer, polarityChoice, text) <- inputLines) {
        val outputID = if (id == "") (idNumStart + numLines).toString else id
        val outputTextList = runThroughPipeLine(text, pipeline)
          .map((s) => s.replaceAll(",", "-COMMA-").replaceAll("\\|", "-PIPE-"))
          .filter(s=>vocab.size==0 || vocab.contains(s))
        numTokens += outputTextList.length
        val outputText = outputTextList.mkString(",")
        polarityChoice match {
          case Left(polarity) =>
            // no targets
            if (polarity != SentimentLabel.Abstained) {
              writeInstance(outputID, reviewer, outputText, polarity.toString, featureWriter)
              numLines += 1
              numClasses(polarity) += 1
              numLabels += 1
            } else {
              numClasses(SentimentLabel.Abstained) += 1
            }
          case Right(polarityMap) =>
            // map of target -> polarity
            val labelList = polarityMap.map {
              case (target, label) => label
            }.toList
            val targetList = polarityMap.map {
              case (target, label) => target
            }.toList
            if (labelList.filter((label) => label != SentimentLabel.Abstained).length > 0) {
              writeInstance(outputID, reviewer, outputText, labelList.mkString(","), featureWriter)
              writeTarget(outputID, targetList.mkString(","), targetWriter)
              numLines += 1
              numLabels += polarityMap.size
              for ((_, label) <- polarityMap) {
                numClasses(label) += 1
              }
            } else {
              numClasses(SentimentLabel.Abstained) += 1
            }
        }
      }
      featureWriter.flush()
      targetWriter.flush()
      logger.info("Stats:\n" +
        "Preprocessed " + numLines + " tweets. " +
        "Used " + numTokens + " tokens. " +
        "Assigned %d labels.\n".format(numLabels) +
        (for ((label, count) <- numClasses if label != SentimentLabel.Abstained) yield
          "%20s: %10d instances (%2.2f%%)"
            .format(
            SentimentLabel.toEnglishName(label),
            count,
            count.toFloat / numLabels * 100)).mkString("\n") +
        "\n\n%20s: %10d instances"
          .format(
          "skipped",
          numClasses(SentimentLabel.Abstained))
      )
      // These may close stdout, so make sure they are last!
      featureWriter.close()
      targetWriter.close()
    }
    catch {
      case e: ArgotUsageException =>
        println(e.message)
        System.exit(1)
    }
  }

}