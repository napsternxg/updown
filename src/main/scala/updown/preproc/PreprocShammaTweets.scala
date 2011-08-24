package updown.preproc

import updown.util._

object PreprocShammaTweets {

  val lineRE = """^(\d+)\t[^\t]+\t([^\t]+)\t([^\t]+)\t[^\t]*\t(.*)$""".r
  val ratingRE = """\d""".r

  val SHAMMA_POS = "2"
  val SHAMMA_NEG = "1"
  val SHAMMA_MIXED = "3"
  val SHAMMA_OTHER = "4"

  // TODO: refactor this into a seperate enum
  val NUM_POS = "1"
  val NUM_NEU = "0"
  val NUM_NEG = "-1"

  def main(args: Array[String]) {
    val lines = scala.io.Source.fromFile(args(0)).getLines
    val stoplist = scala.io.Source.fromFile(args(1)).getLines.toSet

    var numTweets = 0
    var numPosTweets = 0
    var averageIAA = 0.0
    for (line <- lines) {

      val roughTokens = line.split("\t")

      if (!line.startsWith("#") && roughTokens.length >= 8 && line.length > 0 && Character.isDigit(line(0))) {
        val lineRE(tweetid, tweet, username, ratingsRaw) = line
        val ratings = ratingRE.findAllIn(ratingsRaw).toList

        if (ratings.length >= 3) {
          val numPos = ratings.count(_ == SHAMMA_POS)
          val numNeg = ratings.count(_ == SHAMMA_NEG)
          val posFraction = numPos.toFloat / ratings.length
          val negFraction = numNeg.toFloat / ratings.length
          //println(ratings.length)
          //println("posFraction: " + posFraction)
          //println("negFraction: " + negFraction)
          val majorityFraction = math.max(posFraction, negFraction)
          if (majorityFraction > .5) {
            val label = if (posFraction > negFraction) NUM_POS else NUM_NEG
            if (label == NUM_POS) numPosTweets += 1
            numTweets += 1
            averageIAA += majorityFraction

            val tokens = BasicTokenizer(tweet) //TwokenizeWrapper(tweet)
            val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)

            print(tweetid + "|" + username + "|")
            for (feature <- features) print(feature + ",")
            println(label)
          }
        }
      }
    }

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPosTweets.toFloat / numTweets))
    System.err.println("Average inter-annotator agreement: " + averageIAA / numTweets)
  }
}
