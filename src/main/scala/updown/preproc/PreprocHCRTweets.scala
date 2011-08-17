package updown.preproc

import updown.util._
import java.io._
import au.com.bytecode.opencsv.CSVReader
import scala.collection.immutable._

object PreprocHCRTweets {

  def main(args: Array[String]) {
    val reader = new CSVReader(new InputStreamReader(new FileInputStream(new File(args(0))), "UTF-8"))
    val stoplist = scala.io.Source.fromFile(args(1), "utf-8").getLines.toSet 
    val targetWriter = if(args.length >= 3) new OutputStreamWriter(new FileOutputStream(new File(args(2))),"UTF-8") else null
    val featureWriter = if(args.length >= 4 ) new OutputStreamWriter(new FileOutputStream(new File(args(3))),"UTF-8") else null

    /* caution! non-arabic-friendly file io below*/
//    val reader = new CSVReader(new FileReader(args(0))) /* non-arabic-friendly file in*/
//    val stoplist = scala.io.Source.fromFile(args(1)).getLines.toSet /* non-arabic-friendly file in*/ 
//    val targetWriter = if(args.length >= 3) new BufferedWriter(new FileWriter(args(2))) else null /*non arabic friendly file out */

    var numTweets = 0
    var numPos = 0  //takes on a new meaning with multiple target labels
    var numNeg = 0  //same deal here

    var fields = reader.readNext

    while(fields != null) {
      if(fields.length >= 6) {

        try {
	  
          val tweetid = fields(0).toLong.toString.trim // converting to Long and back ensures id is well formed
          val username = fields(2).trim
          val tweet = fields(3).trim
	  val (sentiment1, target1) = (fields(4).trim, fields(5).trim)
	  val (sentiment2, target2) = (fields(6).trim, fields(7).trim)
	  val (sentiment3, target3) = (fields(8).trim, fields(9).trim) 

	  /* A useful check to make sure empty fields are still of type java.lang.String (as are non-empty fields)*/
//	  println("sentiment2: " + sentiment2 + "and the type of that is... "  + sentiment2.getClass.toString)
//	  println("sentiment2 == emptystring " + (sentiment2 == "").toString)
          if((sentiment1 == "positive" || sentiment1 == "negative" || sentiment1 == "neutral" ) && tweetid.length > 0 && username.length > 0 && tweet.length > 0 && target1.length > 0) {  
          
            val tokens = BasicTokenizer(tweet)//TwokenizeWrapper(tweet)
            val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)
            
	    var label1 = ""; var label2 = ""; var label3 = ""
	    var numLabels = 1
	    if (sentiment1 == "positive")  label1 = "1"
	    else if (sentiment1 == "negative")  label1 = "-1"
	    else if (sentiment1 == "neutral")  label1 = "0"
	    
            if(label1 == "1") numPos += 1
	    else if(label1 == "-1") numNeg +=1
	 
/* *** if((sentiment2 == "positive" || sentiment2 == "negative" || sentiment2 == "neutral" ) && target2.length > 0) { 
	      if (sentiment2 == "positive")  label2 = "1"
	      else if (sentiment2 == "negative")  label2 = "-1"
	      else if (sentiment2 == "neutral")  label2 = "0"
	      
              if(label2 == "1") numPos += 1
	      else if(label2 == "-1") numNeg +=1
	      numLabels += 1
	    
	    }
	    
	    if((sentiment3 == "positive" || sentiment3 == "negative" || sentiment3 == "neutral" ) && target3.length > 0) { 
	      if (sentiment3 == "positive")  label3 = "1"
	      else if (sentiment3 == "negative")  label3 = "-1"
	      else if (sentiment3 == "neutral")  label3 = "0"
	      
              if(label3 == "1") numPos += 1
	      else if(label3 == "-1") numNeg +=1
	      numLabels += 1
	    
	    }
*** */	    
	    
            
            
            print(tweetid + "|" + username + "|")
            for(feature <- features) print(feature+",")
//	    if (numLabels == 1) println(label1 + " ")
	    println(label1 + " ") //I think I'm going to axe this trialing space next opportunity i get
/*	    else if(numLabels == 2) println(label1 + "," + label2 + " ")
	    else if(numLabels == 3) println(label1 + "," + label2 + "," + label3 + " ")
*/
            if(targetWriter != null){
              if(numLabels == 1) targetWriter.write(tweetid + "\t" + target1 + "\n")
/*	      else if(numLabels == 2) targetWriter.write(tweetid + "\t" + target1 + "\t" + target2 + "\n")
	      else if(numLabels == 3) targetWriter.write(tweetid + "\t" + target1 + "\t" + target2 + "\t" + target3 + "\n")
*/
	    }
	    if(featureWriter != null){
	      var text = tweetid + "|" + username + "|"
	      for(feature <- features) text += feature + ","
	  
              if(numLabels == 1) text += label1 
/*	      else if(numLabels == 2) text += label1 + "," + label2
	      else if(numLabels == 3) text += label1 + "," + label2 + "," + label3
*/            
	      text += "\n"
	      featureWriter.write(text)
	    }
          }
        }
        catch { case e: Exception => }
      }

      fields = reader.readNext
      numTweets += 1
    }

    reader.close
    if(targetWriter != null) targetWriter.close
    if(featureWriter != null) featureWriter.close

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat/numTweets) + "\tFraction Negative: " + (numNeg.toFloat/numTweets) 
		       + "\tFraction Neutral: " + (1- ((numPos.toFloat/numTweets) +  (numNeg.toFloat/numTweets) ) ).toFloat)
  }
}
