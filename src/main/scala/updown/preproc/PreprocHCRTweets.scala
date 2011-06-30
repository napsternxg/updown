package updown.preproc

import updown.util._
import java.io._
import au.com.bytecode.opencsv.CSVReader

object PreprocHCRTweets {

  def main(args: Array[String]) {

    val reader = new CSVReader(new FileReader(args(0)))
    val targetWriter = if(args.length >= 2) new BufferedWriter(new FileWriter(args(1))) else null

    var fields = reader.readNext

    while(fields != null) {
      if(fields.length >= 6) {

        try {

          val tweetid = fields(0).toLong
          val username = fields(2)
          val tweet = fields(3)
          val sentiment = fields(4)
          val target = fields(5)

          if(sentiment == "positive" || sentiment == "negative") {  
          
            val tokens = TwokenizeWrapper(tweet)
            val features = tokens ::: StringUtil.generateBigrams(tokens)
            
            val label = if(sentiment == "positive") "1" else "-1"
            
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

  }
}
