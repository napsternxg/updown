package updown.preproc

import updown.util._

object PreprocStanfordTweets {

  val lineRE = """^(\d+);;(\d+);;[^;]+;;[^;]+;;([^;]+);;(.*)$""".r

  def main(args: Array[String]) {
    val lines = scala.io.Source.fromFile(args(0)).getLines

    for(line <- lines) {
      val lineRE(sentimentRaw, tweetid, username, tweet) = line

      if(sentimentRaw == "4" || sentimentRaw == "0") {

        val tokens = BasicTokenizer(tweet)
        val features = tokens ::: StringUtil.generateBigrams(tokens)
        
        val label = if(sentimentRaw == "4") "1" else "-1"
      

        print(tweetid + "|" + username + "|")
        for(feature <- features) print(feature+",")
        println(label)
      }
    }
  }
}
