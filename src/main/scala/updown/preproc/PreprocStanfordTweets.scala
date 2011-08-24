package updown.preproc

import updown.util._

object PreprocStanfordTweets {

  val lineRE = """^(\d+);;(\d+);;[^;]+;;[^;]+;;([^;]+);;(.*)$""".r

  // TODO: verify the meanings of these values
  val STAN_POS = "4"
  val STAN_NEU = "2"
  val STAN_NEG = "0"

  // TODO: refactor this into a seperate enum
  val NUM_POS = "1"
  val NUM_NEU = "0"
  val NUM_NEG = "-1"

  def main(args: Array[String]) {
    val lines = scala.io.Source.fromFile(args(0)).getLines
    val stoplist = scala.io.Source.fromFile(args(1)).getLines.toSet

    var numTweets = 0
    var numPos = 0
    for(line <- lines) {
      val lineRE(sentimentRaw, tweetid, username, tweet) = line

      if(sentimentRaw == STAN_POS || sentimentRaw == STAN_NEG) {

        val tokens = BasicTokenizer(tweet)//TwokenizeWrapper(tweet)
        val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)
        
        val label = if(sentimentRaw == STAN_POS) NUM_POS else NUM_NEG
        if(label == NUM_POS) numPos += 1
        
        numTweets += 1
        print(tweetid + "|" + username + "|")
        for(feature <- features) print(feature+",")
        println(label)
      }
    }

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat/numTweets))
  }
}
