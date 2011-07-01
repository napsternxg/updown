package updown.preproc

import updown.util._

object PreprocStanfordTweets {

  val lineRE = """^(\d+);;(\d+);;[^;]+;;[^;]+;;([^;]+);;(.*)$""".r

  def main(args: Array[String]) {
    val lines = scala.io.Source.fromFile(args(0)).getLines
    val stoplist = scala.io.Source.fromFile(args(1)).getLines.toSet

    var numTweets = 0
    var numPos = 0
    for(line <- lines) {
      val lineRE(sentimentRaw, tweetid, username, tweet) = line

      if(sentimentRaw == "4" || sentimentRaw == "0") {

        val tokens = BasicTokenizer(tweet)//TwokenizeWrapper(tweet)
        val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)
        
        val label = if(sentimentRaw == "4") "1" else "-1"
        if(label == "1") numPos += 1
        
        numTweets += 1

        print(tweetid + "|" + username + "|")
        for(feature <- features) print(feature+",")
        println(label)
      }
    }

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat/numTweets))
  }
}
