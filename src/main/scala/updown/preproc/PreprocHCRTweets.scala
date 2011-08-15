package updown.preproc

import updown.util._
import java.io._
import au.com.bytecode.opencsv.CSVReader
import scala.collection.immutable._

object PreprocHCRTweets {

  def main(args: Array[String]) {

    val reader = new CSVReader(new FileReader(args(0)))
    val stoplist = scala.io.Source.fromFile(args(1)).getLines.toSet
    val targetWriter = if(args.length >= 3) new BufferedWriter(new FileWriter(args(2))) else null

    var numTweets = 0
    var numPos = 0  //takes on a new meaning with multiple target labels
    var numNeg = 0  //same deal here

    var fields = reader.readNext

    while(fields != null) {
      if(fields.length >= 6) {

        try {
	  
          val tweetid = fields(0).toLong.toString // converting to Long and back ensures id is well formed
          val username = fields(2)
          val tweet = fields(3)
	  val sentimentTargetPair1 = (fields(4), fields(5))
	  val sentimentTargetPair2 = (fields(6), fields(7))
          val sentimentTargetPair3 = (fields(8), fields(9))
	  val (sentiment1, target1) = sentimentTargetPair1
	  val (sentiment2, target2) = sentimentTargetPair2
	  val (sentiment3, target3) = sentimentTargetPair3

	  /* A useful check to make sure empty fields are still of type java.lang.String (as are non-empty fields)*/
//	  println("sentiment2: " + sentiment2 + "and the type of that is... "  + sentiment2.getClass.toString)
//	  println("sentiment2 == emptystring " + (sentiment2 == "").toString)
          if((sentiment1 == "positive" || sentiment1 == "negative" || sentiment1 == "neutral" ) && tweetid.length > 0 && username.length > 0 && tweet.length > 0 && target1.length > 0) {  
          
            val tokens = BasicTokenizer(tweet)//TwokenizeWrapper(tweet)
            val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)
            
	    var label1 = ""; var label2 = ""; var label3 = ""
	    var numLabels = 1
	    if (sentiment1 == "positive")  label1 = "1"
	    if (sentiment1 == "negative")  label1 = "-1"
	    if (sentiment1 == "neutral")  label1 = "0"
	    
            if(label1 == "1") numPos += 1
	    if(label1 == "-1") numNeg +=1
	 
	    if((sentiment2 == "positive" || sentiment2 == "negative" || sentiment2 == "neutral" ) && target2.length > 0) { 
	      if (sentiment2 == "positive")  label2 = "1"
	      if (sentiment2 == "negative")  label2 = "-1"
	      if (sentiment2 == "neutral")  label2 = "0"
	      
              if(label2 == "1") numPos += 1
	      if(label2 == "-1") numNeg +=1
	      numLabels += 1
	    
	    }
	    
	    if((sentiment3 == "positive" || sentiment3 == "negative" || sentiment3 == "neutral" ) && target3.length > 0) { 
	      if (sentiment3 == "positive")  label3 = "1"
	      if (sentiment3 == "negative")  label3 = "-1"
	      if (sentiment3 == "neutral")  label3 = "0"
	      
              if(label3 == "1") numPos += 1
	      if(label3 == "-1") numNeg +=1
	      numLabels += 1
	    
	    }
	    
	    
            numTweets += 1
            
            print(tweetid + "|" + username + "|")
            for(feature <- features) print(feature+",")
	    if(numLabels == 1) println(label1 + " ")
	    if(numLabels == 2) println(label1 + "," + label2 + " ")
	    if(numLabels == 3) println(label1 + "," + label2 + "," + label3 + " ")

            if(targetWriter != null){
              if(numLabels == 1) targetWriter.write(tweetid + "\t" + target1 + "\n")
	      if(numLabels == 2) targetWriter.write(tweetid + "\t" + target1 + "\t" + target2 + "\n")
	      if(numLabels == 3) targetWriter.write(tweetid + "\t" + target1 + "\t" + target2 + "\t" + target3 + "\n")
	    }
          }
        }
        catch { case e: Exception => }
      }

      fields = reader.readNext
    }

    reader.close
    if(targetWriter != null) targetWriter.close

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat/numTweets) + "\tFraction Negative: " + (numNeg.toFloat/numTweets) 
		       + "\tFraction Neutral: " + (1- ((numPos.toFloat/numTweets) +  (numNeg.toFloat/numTweets) ) ).toFloat)
  }
}
