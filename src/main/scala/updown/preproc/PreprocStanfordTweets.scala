package updown.preproc

import updown.util._

import org.clapper.argot._
import updown.data.SentimentLabel

abstract class TweetParse

case class SuccessfulParse(tweetid: String, username: String, label: SentimentLabel.Type, features: Iterable[String]) extends TweetParse

object PreprocStanfordTweets {
  //IDEA will try to remove this import, but it is not unused. Make sure it stays here.
  // See http://devnet.jetbrains.com/message/5301770;jsessionid=5C12AD4FD62857DAD611E8EEED52DF6A
  import ArgotConverters._
  
  val parser = new ArgotParser("updown run updown.preproc.PreprocStanfordTweets", preUsage = Some("Updown"))

  val inputFile = parser.option[String](List("i", "input"), "input", "path to stanford data file")
  val stopListFile = parser.option[String](List("s", "stoplist"), "stoplist", "path to stoplist file")

  val lineRE = """^(\d+);;(\d+);;[^;]+;;[^;]+;;([^;]+);;(.*)$""".r

  // TODO: verify the meanings of these values
  object StanfordLabel extends Enumeration {
    val Positive = Value("4")
    val Neutral = Value("2")
    val Negative = Value("0")
  }

  def processOneLine(line: String, stoplist: Set[String]): Any = {
    val lineRE(sentimentRaw, tweetid, username, tweet) = line
    val sentiment = StanfordLabel.withName(sentimentRaw)
    if (sentiment == StanfordLabel.Positive || sentiment == StanfordLabel.Negative) {
      val tokens = BasicTokenizer(tweet) //TwokenizeWrapper(tweet)
      val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)
      val label = if (sentiment == StanfordLabel.Positive) SentimentLabel.Positive else SentimentLabel.Negative

      SuccessfulParse(tweetid, username, label, features)
    }
  }

  def main(args: Array[String]) {
    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(0)
    }

    if (inputFile.value == None) {
      println("You must specify a input data file via -i ")
      sys.exit(0)
    }
    if (stopListFile.value == None) {
      println("You must specify a stoplist file via -s ")
      sys.exit(0)
    }

    val lines = scala.io.Source.fromFile(inputFile.value.get).getLines
    val stoplist = scala.io.Source.fromFile(stopListFile.value.get).getLines.toSet

    var numTweets = 0
    var numPos = 0
    for (line <- lines) {
      processOneLine(line, stoplist) match {
        case SuccessfulParse(tweetid, username, label, features) =>
          numTweets += 1
          if (label == SentimentLabel.Positive)
            numPos += 1
          printf("%s|%s|%s,%s\n", tweetid, username, features.mkString(","), label.toString)
        case _ => ()
      }
    }

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat / numTweets))
  }
}
