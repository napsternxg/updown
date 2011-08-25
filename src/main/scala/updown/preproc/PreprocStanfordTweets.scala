package updown.preproc

import updown.util._

import org.clapper.argot._

object PreprocStanfordTweets {
  import ArgotConverters._

  val parser = new ArgotParser("updown run updown.preproc.PreprocStanfordTweets", preUsage=Some("Updown"))
  
  val inputFile = parser.option[String](List("i","input"),"input", "path to stanford data file")
  val stopListFile =  parser.option[String](List("s","stoplist"),"stoplist", "path to stoplist file")

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
    
    try{parser.parse(args)}
    catch { case e: ArgotUsageException => println(e.message); sys.exit(0) }
    
    if(inputFile.value == None){
      println("You must specify a input data file via -i ")
      sys.exit(0)
    }
    if(stopListFile.value == None){
      println("You must specify a stoplist file via -s ")
      sys.exit(0)
    }

    
    val lines = scala.io.Source.fromFile(inputFile.value.get).getLines
    val stoplist = scala.io.Source.fromFile(stopListFile.value.get).getLines.toSet

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
