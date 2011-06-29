package updown.preproc

import updown.util._

object PreprocShammaTweets {

  val lineRE = """^(\d+)\t[^\t]+\t([^\t]+)\t([^\t]+)\t[^\t]*\t(.*)$""".r
  val ratingRE = """\d""".r

  def main(args: Array[String]) {
    val lines = scala.io.Source.fromFile(args(0)).getLines

    for(line <- lines) {

      val roughTokens = line.split("\t")

      if(!line.startsWith("#") && roughTokens.length >= 8 && line.length > 0 && Character.isDigit(line(0))) {
        val lineRE(tweetid, tweet, username, ratingsRaw) = line
        val ratings = ratingRE.findAllIn(ratingsRaw).toList

        //println(tweetid + "  " + tweet + "  " + username)
        //ratings.foreach(println)

        if(ratings.length >= 3) {
          val numPos = ratings.count(_ == "2")
          val numNeg = ratings.count(_ == "1")
          val posFraction = numPos.toFloat / ratings.length
          val negFraction = numNeg.toFloat / ratings.length
          //println(ratings.length)
          //println("posFraction: " + posFraction)
          //println("negFraction: " + negFraction)
          if(math.max(posFraction, negFraction) > .5) {
            val label = if(posFraction > negFraction) "1" else "-1"
            
            val tokens = BasicTokenizer(tweet)
            val features = tokens ::: StringUtil.generateBigrams(tokens)

            print(tweetid + "|" + username + "|")
            for(feature <- features) print(feature+",")
            println(label)
          }
        }
      }

    }
  }
}
