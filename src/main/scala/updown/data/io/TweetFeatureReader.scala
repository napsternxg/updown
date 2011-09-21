package updown.data.io

import updown.data._

object TweetFeatureReader {
  val featureRowRE = """^([^|]*)\|([^|]*)\|([^|]*)\|(.*)$""".r //python verbose regexes are so much nicer :/

  def apply(inputFile: String): List[Tweet] = {

    val lines = scala.io.Source.fromFile(inputFile, "utf-8").getLines.toList

    for (line <- lines) yield {
      parseLine(line)
    }
  }

  def parseLine(line: String): Tweet = {
    val featureRowRE(tweetid, userid, featureString, label) = line
    val features = featureString.split(",").toList.map(_.trim).filter(_.length > 0) // filter out features that are all whitespace or the empty string

    val t = new Tweet(tweetid, userid, features, standardize(label))
    t
  }

  def standardize(label: String): String = {
    label match {
      case "1" => "POS"
      case "-1" => "NEG"
      case "0" => "NEU"
      case _ => label
    }
  }
}
