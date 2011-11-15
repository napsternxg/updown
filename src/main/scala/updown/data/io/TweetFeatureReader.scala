package updown.data.io

import updown.data._

object TweetFeatureReader {
  val featureRowRE = """^([^|]*)\|([^|]*)\|([^|]*)\|(.*)$""".r //python verbose regexes are so much nicer :/

  def apply(inputFile: String): List[GoldLabeledTweet] = {

    val lines = scala.io.Source.fromFile(inputFile, "utf-8").getLines.toList

    for (line <- lines) yield {
      parseLine(line)
    }
  }

  def parseLine(line: String): GoldLabeledTweet = {
    val featureRowRE(tweetid, userid, featureString, label) = line
    val features = featureString.split(",").toList.map(_.trim).filter(_.length > 0) // filter out features that are all whitespace or the empty string

    GoldLabeledTweet(tweetid, userid, features, SentimentLabel.figureItOut(label))
  }
}

/*object RawTweetFeatureReader {
  val featureRowRE = """^([^|]*)\|([^|]*)\|([^|]*)\|(.*)$""".r

  def apply(inputFile: String): List[GoldLabeledTweet] = {

    val lines = scala.io.Source.fromFile(inputFile, "utf-8").getLines.toList

    for (line <- lines) yield {
      parseLine(line: String): GoldLabeledTweet = {
        
      }
    }
  }
*/
