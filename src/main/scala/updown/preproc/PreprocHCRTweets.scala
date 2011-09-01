package updown.preproc

import model.{FailedParse, TweetParse}
import updown.util._
import java.io._
import au.com.bytecode.opencsv.CSVReader
import scala.collection.immutable._

import org.clapper.argot._
import updown.data.SentimentLabel

case class SuccessfulHCRParse(tweetid: String, username: String, label: SentimentLabel.Type, target: String, features: Iterable[String]) extends TweetParse


object PreprocHCRTweets {

  import ArgotConverters._

  val parser = new ArgotParser("updown run updown.preproc.PreprocHCRTweets", preUsage = Some("Updown"))

  val inputFile = parser.option[String](List("in", "input"), "input", "csv input")
  val stopListFile = parser.option[String](List("s", "stoplist"), "stoplist", "stoplist words")
  val targetFile = parser.option[String](List("t", "target"), "target", "target file")
  val featureFile = parser.option[String](List("f", "feature"), "feature", "feature file")

  val HCR_POS = "positive"
  val HCR_NEG = "negative"
  val HCR_NEU = "neutral"

  def processOneLine(fields: Array[String], stoplist: Set[String],
                     targetWriter: OutputStreamWriter, featureWriter: OutputStreamWriter) {
    if (fields.length < 4) {
      return FailedParse
    }

    val tweetid = StringUtil.getLongNoExcept(fields(0).trim)
    val username = fields(2).trim
    val tweet = fields(3).trim
    val sentiment = fields(4).trim
    val target = fields(5).trim

    if (!(sentiment.contains(HCR_POS) || sentiment.contains(HCR_NEG) || sentiment.contains(HCR_NEU))
      || tweetid == "" || username == "" || tweet == "") {
      return FailedParse
    }
    val tokens = BasicTokenizer(tweet)
    val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)
    val label = sentiment match {
      case `HCR_POS` => SentimentLabel.Positive
      case `HCR_NEU` => SentimentLabel.Neutral
      case `HCR_NEG` => SentimentLabel.Negative
    }
    SuccessfulHCRParse(tweetid, username, label, target, features)
  }

  def main(args: Array[String]) {
    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message);
      sys.exit(0)
    }

    if (inputFile.value == None) {
      println("You must specify a input data file via --in or --input ")
      sys.exit(0)
    }
    if (stopListFile.value == None) {
      println("You must specify a stoplist file via -s ")
      sys.exit(0)
    }


    val reader = new CSVReader(new InputStreamReader(new FileInputStream(new File(inputFile.value.get)), "UTF-8"))
    val stoplist = scala.io.Source.fromFile(stopListFile.value.get, "utf-8").getLines.toSet
    val targetWriter = if (targetFile.value != None) new OutputStreamWriter(new FileOutputStream(new File(targetFile.value.get)), "UTF-8") else null
    val featureWriter = if (featureFile.value != None) new OutputStreamWriter(new FileOutputStream(new File(featureFile.value.get)), "UTF-8") else null


    var numTweets = 0
    var numNotCounted = 0
    var numPos = 0 //takes on a new meaning with multiple target labels
    var numNeg = 0 //same deal here
    var numNeu = 0
    var aboveTry = 0
    var noTweetID = 0;
    var noUserName = 0;
    var noTweet = 0
    var noSentiment = 0;
    var noTarget = 0

    var fields = reader.readNext
    var someCount = 0
    var numPassing = 0 //should be same as numTweets. used for debugging.
    while (fields != null) {
      someCount += 1
      processOneLine(fields, stoplist, targetWriter, featureWriter) match {
        case SuccessfulHCRParse(tweetid, username, label, target, features) =>
          ()
        case _ => ()
      }

      fields = reader.readNext

    }

    reader.close
    if (targetWriter != null) targetWriter.close
    if (featureWriter != null) featureWriter.close

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat / numTweets) + "\tFraction Negative: " + (numNeg.toFloat / numTweets)
      + "\tFraction Neutral: " + (1 - ((numPos.toFloat / numTweets) + (numNeg.toFloat / numTweets))).toFloat)
    System.err.println("Num pos tweets: " + numPos + ".\t Num neg tweets: " + numNeg + ".\t Num neutral tweets: " + numNeu)
    System.err.println(numNotCounted + "is numNotCounted" + " and aboveTry is: " + aboveTry + "and num of noSentiment: " + noSentiment + " and num of noTarget " + noTarget)
    System.err.println("noTweet: " + noTweet + " noUserName: " + noUserName + " noTweetID: " + noTweetID)
  }
}
