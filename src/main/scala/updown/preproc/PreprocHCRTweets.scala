package updown.preproc

import model.{TweetParse}
import updown.util._
import java.io._
import au.com.bytecode.opencsv.CSVReader
import scala.collection.immutable._

import org.clapper.argot._
import updown.data.SentimentLabel

case class SuccessfulHCRParse(tweetid: String, username: String, label: SentimentLabel.Type, target: String, features: Iterable[String]) extends TweetParse

case class FailedHCRParse(reason: String) extends TweetParse

object PreprocHCRTweets {

    import ArgotConverters._

  val parser = new ArgotParser("updown run updown.preproc.PreprocHCRTweets", preUsage = Some("Updown"))

  val inputFile = parser.option[String](List("i", "input"), "input", "csv input")
  val stopListFile = parser.option[String](List("s", "stoplist"), "stoplist", "stoplist words")
  val targetFile = parser.option[String](List("t", "target"), "target", "target file")
  val featureFile = parser.option[String](List("f", "feature"), "feature", "feature file")

  val HCR_POS = "positive"
  val HCR_NEG = "negative"
  val HCR_NEU = "neutral"

  val PARSE_FAIL_NO_SENT = "NO_SENT"
  val PARSE_FAIL_INVAL_SENT = "INVAL_SENT"
  val PARSE_FAIL_NO_TWEET_ID = "NO_TWEET_ID"
  val PARSE_FAIL_NO_USERNAME = "NO_USERNAME"
  val PARSE_FAIL_NO_TWEET = "NO_TWEET"
  val PARSE_FAIL_NO_TARGET = "NO_TARGET"

  def processOneLine(fields: Array[String], stoplist: Set[String]): TweetParse = {
    if (fields.length < 5) {
      return FailedHCRParse(PARSE_FAIL_NO_SENT)
    }

    val tweetid = if (fields(0).trim.matches("\\d+")) fields(0).trim else "" // why are we doing this? why not just take whatever is there as the id?
    val username = fields(2).trim
    val tweet = fields(3).trim
    val sentiment = fields(4).trim
    val target = if (fields.length > 5) fields(5).trim else ""

    if (!(sentiment.contains(HCR_POS) || sentiment.contains(HCR_NEG) || sentiment.contains(HCR_NEU)))
      return FailedHCRParse(PARSE_FAIL_INVAL_SENT)
    if (tweetid == "")
      return FailedHCRParse(PARSE_FAIL_NO_TWEET_ID)
    if (username == "")
      return FailedHCRParse(PARSE_FAIL_NO_USERNAME)
    if (tweet == "")
      return FailedHCRParse(PARSE_FAIL_NO_TWEET)
    if (target == "")
      return FailedHCRParse(PARSE_FAIL_NO_TARGET)

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
    val stoplist = scala.io.Source.fromFile(stopListFile.value.get, "utf-8").getLines().toSet
    val targetWriter = if (targetFile.value != None) new OutputStreamWriter(new FileOutputStream(new File(targetFile.value.get)), "UTF-8") else null
    val featureWriter = if (featureFile.value != None) new OutputStreamWriter(new FileOutputStream(new File(featureFile.value.get)), "UTF-8") else null


    var numTweets = 0
    var numNotCounted = 0
    var numPos = 0 //takes on a new meaning with multiple target labels
    var numNeg = 0 //same deal here
    var numNeu = 0
    var noTweetID = 0
    var noUserName = 0
    var noTweet = 0
    var noSentiment = 0
    var noTarget = 0

    var fields = reader.readNext
    while (fields != null) {
      numTweets += 1
      processOneLine(fields, stoplist) match {
        case SuccessfulHCRParse(tweetid, username, label, target, features) =>
          label match {
            case SentimentLabel.Positive => numPos += 1
            case SentimentLabel.Neutral => numNeu += 1
            case SentimentLabel.Negative => numNeg += 1
          }
          if (featureWriter != null)
            featureWriter.write("%s|%s|%s,%s\n".format(tweetid, username, features.mkString(","), label.toString))
          else
            printf("%s|%s|%s,%s\n", tweetid, username, features.mkString(","), label.toString)

          if (targetWriter != null)
            targetWriter.write("%s|%s\n".format(tweetid, target))
          else
            System.err.print("target: %s|%s\n".format(tweetid, target))
        case FailedHCRParse(PARSE_FAIL_NO_SENT) =>
          numNotCounted += 1
          noSentiment += 1
        case FailedHCRParse(PARSE_FAIL_INVAL_SENT) =>
          numNotCounted += 1
          noSentiment += 1
        case FailedHCRParse(PARSE_FAIL_NO_TWEET) =>
          numNotCounted += 1
          noTweet += 1
        case FailedHCRParse(PARSE_FAIL_NO_TWEET_ID) =>
          numNotCounted += 1
          noTweetID += 1
        case FailedHCRParse(PARSE_FAIL_NO_USERNAME) =>
          numNotCounted += 1
          noUserName += 1
        case FailedHCRParse(PARSE_FAIL_NO_TARGET) =>
          numNotCounted += 1
          noTarget += 1
        case _ =>
          numNotCounted += 1
      }

      fields = reader.readNext
    }

    // the convention is that method that have side effects should be called with an empty arg list instead of no parens.
    // these methods are called mutators, apparently
    reader.close()
    if (targetWriter != null) targetWriter.close()
    if (featureWriter != null) featureWriter.close()

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat / numTweets) + "\tFraction Negative: " + (numNeg.toFloat / numTweets)
      + "\tFraction Neutral: " + (1 - ((numPos.toFloat / numTweets) + (numNeg.toFloat / numTweets))).toFloat)
    System.err.println("Num pos tweets: " + numPos + ".\t Num neg tweets: " + numNeg + ".\t Num neutral tweets: " + numNeu)
    System.err.println(numNotCounted + "is numNotCounted" + "and num of noSentiment: " + noSentiment + " and num of noTarget " + noTarget)
    System.err.println("noTweet: " + noTweet + " noUserName: " + noUserName + " noTweetID: " + noTweetID)
  }
}
