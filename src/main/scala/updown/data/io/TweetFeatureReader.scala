package updown.data.io

import updown.data._

object TweetFeatureReader {

  val featureRowRE = """^([^|]+)\|([^|]+)\|(.*),([^,]+)$""".r

  def apply(inputFile: String):List[Tweet] = {

    val lines = scala.io.Source.fromFile(inputFile).getLines.toList

    for(line <- lines) yield {
      val featureRowRE(tweetid, userid, featureString, label) = line
      val features = featureString.split(",").toList.map(_.trim).filter(_.length > 0) // filter out features that are all whitespace or the empty string

      new Tweet(tweetid, userid, features, standardize(label))
    }
  }

  def standardize(label: String): String = {
    label match {
      case "1" => "POS"
      case "-1" => "NEG"
      case _ => label
    }
  }
}
