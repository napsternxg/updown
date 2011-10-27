package updown.preproc

import model.TweetParse
import updown.util._

import org.clapper.argot._
import updown.data.SentimentLabel

case class SuccessfulShammaParse(tweetid: String, username: String, label: SentimentLabel.Type, iaa: Double, features: Iterable[String]) extends TweetParse

object PreprocShammaTweets {
  import ArgotConverters._

  val parser = new ArgotParser("updown preproc-shamma", preUsage=Some("Updown"))
  
  val inputFile = parser.option[String](List("i","input"),"input", "path to Shamma's Obama-McCain debate data file")
  val stopListFile =  parser.option[String](List("s","stoplist"),"stoplist", "path to stoplist file")
  
  val lineRE = """^(\d+)\t[^\t]+\t([^\t]+)\t([^\t]+)\t[^\t]*\t(.*)$""".r
  val ratingRE = """\d""".r

  val SHAMMA_POS = "2"
  val SHAMMA_NEG = "1"
  val SHAMMA_MIXED = "3"
  val SHAMMA_OTHER = "4"

  def processOneLine(line: String, stoplist: Set[String]): Any = {
    val roughTokens = line.split("\t")

    if (!line.startsWith("#") && roughTokens.length >= 8 && line.length > 0 && Character.isDigit(line(0))) {
      val lineRE(tweetid, tweet, username, ratingsRaw) = line
      val ratings = ratingRE.findAllIn(ratingsRaw).toList

      // we only consider tweets that were evaluated by 3 or more annotators
      if (ratings.length >= 3) {
        val numPos = ratings.count(_ == SHAMMA_POS)
        val numNeg = ratings.count(_ == SHAMMA_NEG)
        val posFraction = numPos.toFloat / ratings.length
        val negFraction = numNeg.toFloat / ratings.length
        val majorityFraction = math.max(posFraction, negFraction)
        //only consider non-tie classifications
        if (majorityFraction > .5) {
          val label = if (posFraction > negFraction) SentimentLabel.Positive else SentimentLabel.Negative
          val tokens = BasicTokenizer(tweet)
          val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)

          SuccessfulShammaParse(tweetid, username, label, majorityFraction, features)
        }
      }
    }
  }

  def main(args: Array[String]) {
    try{parser.parse(args)}
    catch { case e: ArgotUsageException => println(e.message); sys.exit(0) }
    
    if(inputFile.value == None){
      println("You must specify a input data file via -i")
      sys.exit(0)
    }
    if(stopListFile.value == None){
      println("You must specify a stoplist file via -s ")
      sys.exit(0)
    }
    
    val lines = scala.io.Source.fromFile(inputFile.value.get).getLines
    val stoplist = scala.io.Source.fromFile(stopListFile.value.get).getLines.toSet

    var numTweets = 0
    var numPosTweets = 0
    var averageIAA = 0.0
    for (line <- lines) {

      processOneLine(line, stoplist) match {
        case SuccessfulShammaParse(tweetid, username, label, iaa, features) =>
          numTweets += 1
          averageIAA += iaa
          if (label == SentimentLabel.Positive) numPosTweets += 1
          printf("%s|%s|%s|%s\n", tweetid, username, features.mkString(",").replace("|", ""), label.toString)
        case _ => ()
      }
    }

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPosTweets.toFloat / numTweets))
    System.err.println("Average inter-annotator agreement: " + averageIAA / numTweets)
  }
}
