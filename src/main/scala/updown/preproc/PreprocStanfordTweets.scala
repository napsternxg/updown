package updown.preproc

import model.TweetParse
import updown.util._

import org.clapper.argot._
import updown.data.SentimentLabel

case class SuccessfulStanfordParse(tweetid: String, username: String, label: SentimentLabel.Type, features: Iterable[String]) extends TweetParse

object PreprocStanfordTweets {
  //IDEA will try to remove this import, but it is not unused. Make sure it stays here.
  // See http://devnet.jetbrains.com/message/5301770;jsessionid=5C12AD4FD62857DAD611E8EEED52DF6A
  import ArgotConverters._
  
  val parser = new ArgotParser("updown run updown.preproc.PreprocStanfordTweets", preUsage = Some("Updown"))

  val inputFile = parser.option[String](List("i", "input"), "input", "path to stanford data file")
  val stopListFile = parser.option[String](List("s", "stoplist"), "stoplist", "path to stoplist file")

  val lineRE = """^(\d+);;(\d+);;[^;]+;;[^;]+;;([^;]+);;(.*)$""".r

  // TODO: verify the meanings of these values
  val STAN_POS = "4"
  val STAN_NEU = "2"
  val STAN_NEG = "0"

  def processOneLine(line: String, stoplist: Set[String]): Any = {
    val lineRE(sentimentRaw, tweetid, username, tweet) = line
    if (sentimentRaw == STAN_POS || sentimentRaw == STAN_NEG) {
      val tokens = BasicTokenizer(tweet)
      val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)
      val label = if (sentimentRaw == STAN_POS) SentimentLabel.Positive else SentimentLabel.Negative

      SuccessfulStanfordParse(tweetid, username, label, features)
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
        case SuccessfulStanfordParse(tweetid, username, label, features) =>
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
