package updown.preproc

import updown.util._
import java.io._
import au.com.bytecode.opencsv.CSVReader

object PreprocHCRTweets {

  def main(args: Array[String]) {

    val reader = new CSVReader(new FileReader(args(0)))
    val stoplist = scala.io.Source.fromFile(args(1)).getLines.toSet
    val targetWriter = if(args.length >= 3) new BufferedWriter(new FileWriter(args(2))) else null

    var numTweets = 0
    var numPos = 0

    var fields = reader.readNext

    while(fields != null) {
      if(fields.length >= 6) {

        try {

          val tweetid = fields(0).toLong.toString // converting to Long and back ensures id is well formed
          val username = fields(2)
          val tweet = fields(3)
          val sentiment = fields(4)
          val target = fields(5)

          if((sentiment == "positive" || sentiment == "negative") && tweetid.length > 0 && username.length > 0 && tweet.length > 0 && target.length > 0) {  
          
            val tokens = TwokenizeWrapper(tweet)
            val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)
            
            val label = if(sentiment == "positive") "1" else "-1"
            if(label == "1") numPos += 1
            numTweets += 1
            
            print(tweetid + "|" + username + "|")
            for(feature <- features) print(feature+",")
            println(label)

            if(targetWriter != null)
              targetWriter.write(tweetid + "\t" + target + "\n")
          }
        }
        catch { case e: Exception => }
      }

      fields = reader.readNext
    }

    reader.close
    if(targetWriter != null) targetWriter.close

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat/numTweets))
  }
}
