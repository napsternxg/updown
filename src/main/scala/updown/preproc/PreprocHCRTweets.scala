package updown.preproc

import updown.util._
import java.io._
import au.com.bytecode.opencsv.CSVReader
import scala.collection.immutable._

object PreprocHCRTweets {

  val HCR_POS = "positive"
  val HCR_NEG = "negative"
  val HCR_NEU = "neutral"

  // TODO: refactor this into a seperate enum
  val NUM_POS = "1"
  val NUM_NEU = "0"
  val NUM_NEG = "-1"

  def main(args: Array[String]) {
    val reader = new CSVReader(new InputStreamReader(new FileInputStream(new File(args(0))), "UTF-8"))
    val stoplist = scala.io.Source.fromFile(args(1), "utf-8").getLines.toSet
    val targetWriter = if(args.length >= 3) new OutputStreamWriter(new FileOutputStream(new File(args(2))),"UTF-8") else null
    val featureWriter = if(args.length >= 4 ) new OutputStreamWriter(new FileOutputStream(new File(args(3))),"UTF-8") else null


    var numTweets = 0
    var numNotCounted = 0
    var numPos = 0  //takes on a new meaning with multiple target labels
    var numNeg = 0  //same deal here
    var numNeu = 0
    var aboveTry = 0
    var noTweetID = 0; var noUserName = 0; var noTweet = 0
    var noSentiment = 0;    var noTarget = 0

    var fields = reader.readNext
    var someCount = 0 
    var numPassing = 0 //should be same as numTweets. used for debugging.
    while(fields != null) {
      someCount += 1
      if(fields.length >= 4) {
	aboveTry += 1
        try {
	  var  tweetid = ""; var username = ""; var tweet = ""; var sentiment1 = ""; var target1 = ""
	  try{
            tweetid = fields(0).toLong.toString.trim // converting to Long and back ensures id is well formed
	  }
	  catch{case _ => noTweetID += 1; tweetid = "NONE"}
	  try{
            username = fields(2).trim
	  }
	  catch{case _ => noUserName += 1; username ="NONE"}
	  try{
          tweet = fields(3).trim
	  }
	  catch{case _ => noTweet += 1}
	  try{
	    sentiment1 = fields(4).trim
	  }
	  catch{case _ => sentiment1 = "NONE";noSentiment += 1}
	  try{
	  target1 = fields(5).trim
	  }
	  catch{case _ => target1 ="NONE"; noTarget += 1}
	 
	  
	  
	  numTweets += 1
            if((sentiment1.contains(HCR_POS) 
		|| sentiment1.contains(HCR_NEG) 
		|| sentiment1.contains(HCR_NEU)) 
	       && tweetid.length > 0 && username.length > 0 && tweet.length > 0) {  
              numPassing += 1 
	      //println("numPassing: " + numPassing + " and numTweets is " + numTweets + " and someCount is " + someCount)
              val tokens = BasicTokenizer(tweet)//TwokenizeWrapper(tweet)
              val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)
              
	      var label1 = ""; //var label2 = ""; var label3 = ""
	      var numLabels = 1
	      if (sentiment1 == HCR_POS)  label1 = NUM_POS
	      else if (sentiment1 == HCR_NEG)  label1 = NUM_NEG
	      else if (sentiment1 == HCR_NEU)  label1 = NUM_NEU

	      
              if(label1 == NUM_POS) numPos += 1
	      else if(label1 == NUM_NEG) numNeg += 1
	      else if(label1 == NUM_NEU) numNeu += 1
	      /* A lot of multiple target stuff below has been commented out and will be uncommented when this functionality is ready... */
	      
	      /* *** if((sentiment2 == HCR_POS || sentiment2 == HCR_NEG || sentiment2 == HCR_NEU ) && target2.length > 0) { 
	       if (sentiment2 == HCR_POS)  label2 = NUM_POS
	       else if (sentiment2 == HCR_NEG)  label2 = NUM_NEG
	       else if (sentiment2 == HCR_NEU)  label2 = NUM_NEU
	       
               if(label2 == NUM_POS) numPos += 1
	       else if(label2 == NUM_NEG) numNeg +=1
	       numLabels += 1
	       
	       }
	       
	       if((sentiment3 == HCR_POS || sentiment3 == HCR_NEG || sentiment3 == HCR_NEU ) && target3.length > 0) { 
	       if (sentiment3 == HCR_POS)  label3 = NUM_POS
	       else if (sentiment3 == HCR_NEG)  label3 = NUM_NEG
	       else if (sentiment3 == HCR_NEU)  label3 = NUM_NEU
	       
               if(label3 == NUM_POS) numPos += 1
		 else if(label3 == NUM_NEG) numNeg +=1
		 numLabels += 1
		 
		 }
		 *** */	    
	      
              
              /*below print lines are important */

//              print(tweetid + "|" + username + "|") 
//              for(feature <- features) print(feature+",")
//	      println(label1)//yeah, i chopped off the added space.. 
	      

	      /* ***	    if (numLabels == 1) println(label1 + " ")
	       * else if(numLabels == 2) println(label1 + "," + label2 + " ")
	       else if(numLabels == 3) println(label1 + "," + label2 + "," + label3 + " ")
	       *** */


              if(targetWriter != null){
		if(numLabels == 1) targetWriter.write(tweetid + "\t" + target1 + "\n")

		/*else if(numLabels == 2) targetWriter.write(tweetid + "\t" + target1 + "\t" + target2 + "\n")
		 else if(numLabels == 3) targetWriter.write(tweetid + "\t" + target1 + "\t" + target2 + "\t" + target3 + "\n")
		 */
	      }
	      
	      /*writing to output file in utf-8 works better than print line statements for non-ASCII values :) */
	      if(featureWriter != null){
		var text = tweetid + "|" + username + "|"
		for(feature <- features) text += feature + ","
		var textStripped = text.replaceAll("\n","").replaceAll("\r","").replaceAll("\t","")
		if(numLabels == 1) textStripped += label1 
		
		
		/*else if(numLabels == 2) textStripped += label1 + "," + label2
		 else if(numLabels == 3) textStripped += label1 + "," + label2 + "," + label3
		 */            
		
		featureWriter.write(textStripped + "\n")
	      }
              //numTweets += 1
	      System.err.println(numNotCounted + " " + tweetid + " " + username + " " + tweet + " " + sentiment1 + " " )
	    }
		else{
		  numNotCounted +=1
		  //System.err.println(numNotCounted + " " + tweetid + " " + username + " " + tweet + " " + sentiment1 + " " )
		}
	}
        catch {
          case e: Exception => System.err.println("Error processing tweet at line #" + (numTweets + 1)
            +": "+java.util.Arrays.toString(fields.asInstanceOf[Array[java.lang.Object]]))
        }
      }

      fields = reader.readNext
    
    }

    reader.close
    if(targetWriter != null) targetWriter.close
    if(featureWriter != null) featureWriter.close

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat/numTweets) + "\tFraction Negative: " + (numNeg.toFloat/numTweets) 
		       + "\tFraction Neutral: " + (1- ((numPos.toFloat/numTweets) +  (numNeg.toFloat/numTweets) ) ).toFloat)
    System.err.println("Num pos tweets: " + numPos +".\t Num neg tweets: " + numNeg + ".\t Num neutral tweets: " + numNeu)
    System.err.println(numNotCounted + "is numNotCounted" + " and aboveTry is: " + aboveTry + "and num of noSentiment: " + noSentiment + " and num of noTarget " + noTarget)
    System.err.println("noTweet: " + noTweet + " noUserName: " + noUserName + " noTweetID: " + noTweetID)
  }
}
