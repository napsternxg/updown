package updown.preproc

import model.{TweetParse}
import updown.util._
import java.io._
import au.com.bytecode.opencsv.CSVReader
import scala.collection.immutable._

import org.clapper.argot._
import updown.data.SentimentLabel

case class SuccessfulHCRParse(tweetid: String, username: String,
                              sentTargList: List[(SentimentLabel.Type, String)],
                              features: Iterable[String]) extends TweetParse

case class FailedHCRParse(reason: String) extends TweetParse

object PreprocHCRTweets {

  import ArgotConverters._

  val parser = new ArgotParser("updown run updown.preproc.PreprocHCRTweets", preUsage = Some("Updown"))

  val inputFile = parser.option[String](List("i", "input"), "input", "csv input")
  val stopListFile = parser.option[String](List("s", "stoplist"), "stoplist", "stoplist words")
  val targetFile = parser.option[String](List("t", "target"), "target", "target file")
  val featureFile = parser.option[String](List("f", "feature"), "feature", "feature file")
  val ignoreNeutral = parser.flag[Boolean](List("ignoreNeutral"), "set this flag if you want to ignore neutral annotations")

  val HCR_POS = "positive"
  val HCR_NEG = "negative"
  val HCR_NEU = "neutral"

  val PARSE_FAIL_NO_SENT = "NO_SENT"
  val PARSE_FAIL_INVAL_SENT = "INVAL_SENT"
  val PARSE_FAIL_NO_TWEET_ID = "NO_TWEET_ID"
  val PARSE_FAIL_NO_USERNAME = "NO_USERNAME"
  val PARSE_FAIL_NO_TWEET = "NO_TWEET"
  val PARSE_FAIL_NO_TARGET = "NO_TARGET"

  def processOneLine(numFields: Int, fields: Array[String], stoplist: Set[String]): TweetParse = {
    // tweet id,user id,username,content,sentiment,target,annotator id,comment,dispute
    val INDEX_TWEET_ID = 0
    //val INDEX_USER_ID = 1 // unused, leaving here for documentation
    val INDEX_USER_NAME = 2
    val INDEX_CONTENT = 3

    val ITERATE_START = 4
    val ITERATE_END = numFields - 3

    //    val INDEX_ANNOTATOR_ID = numFields - 3 // unused, leaving here for documentation
    //    val INDEX_COMMENT = numFields - 2 // unused, leaving here for documentation
    //    val INDEX_DISPUTE = numFields - 1 // unused, leaving here for documentation

    if (fields.length < 5) {
      return FailedHCRParse(PARSE_FAIL_NO_SENT)
    }

    val tweetid = if (fields(INDEX_TWEET_ID).trim.matches("\\d+")) fields(INDEX_TWEET_ID).trim else "" // why are we doing this? why not just take whatever is there as the id?
    val username = if (fields.length > INDEX_USER_NAME) fields(INDEX_USER_NAME).trim else ""
    val tweet = if (fields.length > INDEX_CONTENT) fields(INDEX_CONTENT).trim else ""
    var sentimentList = List[SentimentLabel.Type]()
    var targetList = List[String]()
    for (i <- ITERATE_START until ITERATE_END by 2) {
      val sentiment = if (fields.length > i) fields(i).trim else ""
      if (!(sentiment == HCR_POS || sentiment == HCR_NEG || sentiment == HCR_NEU))
        return FailedHCRParse(PARSE_FAIL_INVAL_SENT)

      sentimentList = (sentiment match {
        case `HCR_POS` => SentimentLabel.Positive
        case `HCR_NEU` => SentimentLabel.Neutral
        case `HCR_NEG` => SentimentLabel.Negative
      }) :: sentimentList

      targetList = (if (fields.length > i + 1) fields(i + 1).trim else "") :: targetList
      if (targetList(0) == "")
        return FailedHCRParse(PARSE_FAIL_NO_TARGET)
    }
    val sentTargList = sentimentList zip targetList

    if (tweetid == "")
      return FailedHCRParse(PARSE_FAIL_NO_TWEET_ID)
    if (username == "")
      return FailedHCRParse(PARSE_FAIL_NO_USERNAME)
    if (tweet == "")
      return FailedHCRParse(PARSE_FAIL_NO_TWEET)

    val tokens = BasicTokenizer(tweet)
    val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)

    SuccessfulHCRParse(tweetid, username, sentTargList, features)
  }

  def writeOutput(featureWriter: OutputStreamWriter, tweetid: String,
                  username: String, features: Iterable[String],
                  sentTargList: List[(SentimentLabel.Type, String)], targetWriter: OutputStreamWriter) {
    var label = ""
    var target = ""
    for ((sentiment, targetString) <- sentTargList) {
      label += (if (label != "") "," else "") + sentiment
      target += (if (target != "") "," else "") + targetString
    }
    featureWriter.write("%s|%s|%s,%s\n".format(tweetid, username, features.mkString(","), label))
    targetWriter.write("%s|%s\n".format(tweetid, target))
  }

  def main(args: Array[String]) {
    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message);
      sys.exit(0)
    }

    // dumb, I know, but a boolean flag turns out to be an Option, which is even dumber
    val ignoreNeut = if (ignoreNeutral.value == None) false else true

    if (inputFile.value == None) {
      println("You must specify a input data file via --in or --input ")
      sys.exit(0)
    }
    if (stopListFile.value == None) {
      println("You must specify a stoplist file via -s ")
      sys.exit(0)
    }


    val reader = new CSVReader(new InputStreamReader(new FileInputStream(new File(inputFile.value.get)), "UTF-8"))
    val stoplist = scala.io.Source.fromFile(stopListFile.value.get, "utf-8").getLines().toSet
    val targetWriter = new OutputStreamWriter(
      (if (targetFile.value != None)
        new FileOutputStream(new File(targetFile.value.get))
      else
        System.err), "UTF-8")

    val featureWriter = new OutputStreamWriter(
      (if (featureFile.value != None)
        new FileOutputStream(new File(featureFile.value.get))
      else
        System.out), "UTF-8")


    var numTweets = 0
    var numCounted = 0
    var numPos = 0 //takes on a new meaning with multiple target labels
    var numNeg = 0 //same deal here
    var numNeu = 0
    var noTweetID = 0
    var noUserName = 0
    var noTweet = 0
    var noSentiment = 0
    var invalSentiment = 0
    var noTarget = 0


    var fields = reader.readNext
    // Assumes there is a header!!!
    val numFields = fields.length
    fields = reader.readNext
    while (fields != null) {
      numTweets += 1
      processOneLine(numFields, fields, stoplist) match {
        case SuccessfulHCRParse(tweetid, username, sentTargList, features) =>
          for ((sentiment, target) <- sentTargList) {
            numCounted += 1
            sentiment match {
              case SentimentLabel.Positive => numPos += 1
              case SentimentLabel.Negative => numNeg += 1
              case SentimentLabel.Neutral =>
                if (!ignoreNeut)
                  numNeu += 1
                else
                  numCounted -= 1
            }
          }
          writeOutput(featureWriter, tweetid, username, features, sentTargList, targetWriter)
        case FailedHCRParse(PARSE_FAIL_NO_SENT) =>
          noSentiment += 1
        case FailedHCRParse(PARSE_FAIL_INVAL_SENT) =>
          invalSentiment += 1
        case FailedHCRParse(PARSE_FAIL_NO_TWEET) =>
          noTweet += 1
        case FailedHCRParse(PARSE_FAIL_NO_TWEET_ID) =>
          noTweetID += 1
        case FailedHCRParse(PARSE_FAIL_NO_USERNAME) =>
          noUserName += 1
        case FailedHCRParse(PARSE_FAIL_NO_TARGET) =>
          noTarget += 1
      }
      fields = reader.readNext
    }

    targetWriter.flush()
    featureWriter.flush()

    val log = System.err

    log.println("Preprocessed " + numCounted +
      " tweets. Fraction positive: " + (numPos.toFloat / numCounted) +
      "\tFraction Negative: " + (numNeg.toFloat / numCounted)
      + "\tFraction Neutral: " + (numNeu.toFloat / numCounted))
    log.println("Num pos tweets: " + numPos + ".\t Num neg tweets: " + numNeg + ".\t Num neutral tweets: " + numNeu)
    log.println((numTweets - numCounted) + " were numNotCounted" +
      "\nand num of noSentiment: " + noSentiment +
      "\nand num of invalSentiment: " + invalSentiment +
      "\nand num of noTarget " + noTarget)
    log.println("noTweet: " + noTweet + " noUserName: " + noUserName + " noTweetID: " + noTweetID)

    reader.close()
    targetWriter.close()
    featureWriter.close()

  }
}
