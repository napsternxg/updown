package updown.preproc.impl

import updown.data.SentimentLabel
import updown.preproc.GenericPreprocessor
object PreprocShammaTweets extends GenericPreprocessor {
  //IDEA will try to remove this import, but it is not unused. Make sure it stays here.
  // See http://devnet.jetbrains.com/message/5301770;jsessionid=5C12AD4FD62857DAD611E8EEED52DF6A

  val lineRE = """^(\d+)\t[^\t]+\t([^\t]+)\t([^\t]+)\t[^\t]*\t(.*)$""".r
  val ratingRE = """\d""".r

  val SHAMMA_POS = "2"
  val SHAMMA_NEG = "1"
  val SHAMMA_MIXED = "3"
  val SHAMMA_OTHER = "4"

  override val defaultPipeline = "basicTokenize|addBigrams|removeStopwords"


  def getSingleRating(ratings: List[String]): SentimentLabel.Type = {
    // we only consider tweets that were evaluated by 3 or more annotators
    if (ratings.length >= 3) {
      val fracPos = ratings.count(_ == SHAMMA_POS).toFloat / ratings.length
      val fracNeg = ratings.count(_ == SHAMMA_NEG).toFloat / ratings.length

      //only consider non-tie classifications
      if (fracPos > .5 && fracPos > fracNeg) {
        SentimentLabel.Positive
      } else if (fracNeg > .5 && fracNeg > fracPos) {
        SentimentLabel.Negative
      } else {
        SentimentLabel.Abstained
      }
    } else {
      SentimentLabel.Abstained
    }
  }

  def getInstanceIterator(fileName: String, polarity: String): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {
    for (line <- scala.io.Source.fromFile(fileName, "UTF-8").getLines) yield {
      val roughTokens = line.split("\t")

      if (!line.startsWith("#") && roughTokens.length >= 8 && line.length > 0 && Character.isDigit(line(0))) {
        val lineRE(id, tweet, username, ratingsRaw) = line

        val label = getSingleRating(ratingRE.findAllIn(ratingsRaw).toList)
        logger.debug("id:%s label:%s".format(id, SentimentLabel.toEnglishName(label)))
        (id, username, Left(label), tweet)
      } else {
        ("","",Left(SentimentLabel.Abstained),"")
      }
    }
  }
}
