package updown.app.experiment.topic.pam

import org.clapper.argot.ArgotParser._
import org.clapper.argot.ArgotConverters._
import java.io.{FileWriter, BufferedWriter, File}
import updown.util.{HPAMTopicModel, WordleUtils, LDATopicModel, TopicModel}
import updown.app.experiment.{LabelResult, ExperimentalResult, NFoldExperiment}
import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}

abstract class NFoldPAMExperiment extends NFoldExperiment {
  var iterations = 1000
//  var alpha = 30
//  var beta = 0.1
  var numSuperTopics = 3
  var numSubTopics = 20
  val fileSeparator = System.getProperty("file.separator")

  var childProcesses = List[Process]()

  val iterationOption = parser.option[Int](List("iterations"), "INT", "the number of iterations for the training the topicModel")
//  val alphaOption = parser.option[Int](List("alpha"), "INT", "the symmetric alpha hyperparameter for LDA")
//  val betaOption = parser.option[Double](List("beta"), "DOUBLE", "the symmetric beta hyperparameter for LDA")
  val numSuperTopicsOption = parser.option[Int](List("numSuperTopics"), "INT", "the number of supertopics for PAM")
  val numSubTopicsOption = parser.option[Int](List("numSubTopics"), "INT", "the number of subtopics for PAM")

  val outputOption = parser.option[String](List("o", "output"), "DIR", "the directory to dump topics into")
  val wordleOption = parser.flag[Boolean](List("w", "wordle"), "generate wordles for the topics (requires -o DIR) " +
    "(requires that you have downloaded IBM's word cloud generator)")
  val wordleJarOption = parser.option[String](List("wordleJar"), "PATH", ("the path to IBM's word cloud generator " +
    "(default %s)").format(WordleUtils.defaultJarPath))
  val wordleConfigOption = parser.option[String](List("wordleConfig"), "PATH", ("the path to the config file for IBM's " +
    "word cloud generator (default %s)").format(WordleUtils.defaultConfigurationPath))

  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]): List[SystemLabeledTweet]

  def doOutput(model: TopicModel) {
    if (outputOption.value.isDefined) {
      val file = new File(outputOption.value.get + fileSeparator + "run" + experimentalRun)
      file.mkdirs()
      val outputDirForThisRun = file.getAbsolutePath
      val summary = new BufferedWriter((new FileWriter((outputDirForThisRun + fileSeparator + "summary"))))
      summary.write("%s\n".format(model.getTopicPriors.zipWithIndex.map {
        case (a, b) => "Topic %s:%6.3f".format(b, a)
      }.mkString("\n")))
      summary.write("%s\n".format(model.getLabelsToTopicDist.toList.map {
        case (a, b) => "Label %9s:%s".format(SentimentLabel.toEnglishName(a), b.map {
          "%7.3f".format(_)
        }.mkString(""))
      }.mkString("\n")))
      summary.close()
      val outputFiles =
        (for ((topic, i) <- model.getTopics.zipWithIndex) yield {
          val outFile = new File(outputDirForThisRun + fileSeparator + "topic" + i)
          val output = new BufferedWriter(new FileWriter(outFile))
          output.write("%s\n".format(topic.distribution.toList.sortBy((pair) => (1 - pair._2)).map {
            case (a, b) => "%s\t%s".format(a, b)
          }.mkString("\n")))
          output.close()
          outFile.getAbsolutePath
        })
      if (wordleOption.value.isDefined) {
        logger.debug("making wordles and report")
        val index = new BufferedWriter((new FileWriter((outputDirForThisRun + fileSeparator + "index.html"))))
        index.write("<head><style>\n%s\n</style></head>\n".format(List(
        "div.bordered{border-style: solid none none none; padding: 5px; border-width: 1px; border-color: gray;}",
        "div#wordles{display:block; clear:both; padding-top:20px;}",
        "div.wordle{float:left;width:45%;border-style:solid; border-width:1px; border-color:gray; margin:2px;}",
        "div.wordle img{width: 100%;}",
        ".table{display:block; clear: both;}",
        ".row{display:block;clear:both;}",
        ".cell{display:block;float:left;}",
        ".values{display:block;float:left;width:300px;}",
        ".value{display:block;float:left;width:60px;}",
        "div.topicFreq .title{width:100px;}",
        "div.labelDistribution .title{width:150px;}"
        ).mkString("\n")))
        index.write("<body>")
        index.write("<div id=topicDistribution class=\"bordered table\">%s</div>\n".format(model.getTopicPriors.zipWithIndex.map {
          case (a, b) => "<div class=\"topicFreq row\"><span class=\"title cell\">Topic %s</span><span class=\"value cell\">%6.3f</span></div>".format(b, a)
        }.mkString("\n")))
        index.write(("<div id=labelDistributions class=\"bordered table\">" +
          "<div class=\"labelDistribution row\"><span class=\"title cell\">topic</span><span class=\"values cell\"><span class=\"value\">  0</span><span class=\"value\">  1</span><span class=\"value\">  2</span></span></div>" +
          "%s</div>\n").format(model.getLabelsToTopicDist.toList.sortBy({case(a,b)=>SentimentLabel.ordinality(a)}).map {
          case (a, b) => "<div class=\"labelDistribution row\"><span class=\"title cell\">Label %9s</span><span class=\"values cell\">%s</span></div>".format(SentimentLabel.toEnglishName(a), b.map {
            "<span class=value>%7.3f</span>".format(_)
          }.mkString(""))
        }.mkString("\n")))
        val jarPath = if (wordleJarOption.value.isDefined) wordleJarOption.value.get else WordleUtils.defaultJarPath
        val configPath = if (wordleConfigOption.value.isDefined) wordleConfigOption.value.get else WordleUtils.defaultConfigurationPath
        index.write("<div id=wordles class=bordered>")
        childProcesses = childProcesses ::: WordleUtils.makeWordles(jarPath, configPath, outputFiles, Some(index))
        index.write("</div></body>")
        index.close()
        logger.debug("done making report and initializing wordles")
      }
    }
  }

  def doExperiment(testSet: List[GoldLabeledTweet], trainSet: List[GoldLabeledTweet]) = {
    if (iterationOption.value.isDefined) {
      iterations = iterationOption.value.get
    }
    if (numSuperTopicsOption.value.isDefined) {
      numSuperTopics = numSuperTopicsOption.value.get
    }
    if (numSubTopicsOption.value.isDefined) {
      numSubTopics = numSubTopicsOption.value.get
    }
    val model: TopicModel = new HPAMTopicModel(trainSet, numSuperTopics, numSubTopics, iterations)
    logger.info("topicString:\n"+model.toString)
    
    evaluate(model, testSet)
  }

  def after(): Int = {
    if (childProcesses.length > 0) {
      logger.info("waiting for child processes...")
      WordleUtils.waitForChildren(childProcesses)
    } else {
      0
    }
  }
}