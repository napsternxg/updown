package updown.data

import java.io._
import java.util.zip._
import org.apache.tools.bzip2._
import org.json._
import updown.util._
//import scala.util.parsing.json._

object NgramProbabilityCalculator {

  val COUNT_THRESHOLD = 5.0
  //val underThresholdProbs = new scala.collection.mutable.HashMap[String, Double] { override def default(s: String) = 0.0 }
  //val ngramProbabilities = new scala.collection.mutable.HashMap[String, Double] { override def default(s: String) = 0.0 }
  val probLex = new ProbabilityLexicon
  var tweetCount = 0
  var tweetsToProcess = 1000
  //var wordCount = 0

  def main(args: Array[String]) = {
    val inFile = new File(args(0))
    tweetsToProcess = args(2).toInt

    if(inFile.isDirectory) {
      processDir(inFile)
    }
    else {
      processFile(inFile)
    }

    //ngramProbabilities.foreach(p => if(p._2 < COUNT_THRESHOLD) ngramProbabilities.remove(p._1))
    
    //val ngramProbabilitiesPruned = ngramProbabilities.filter(_._2 >= COUNT_THRESHOLD)

    //ngramProbabilities/*Pruned*/.foreach(p => ngramProbabilities/*Pruned*/.put(p._1, p._2 / wordCount))

    println("Final word count was " + probLex.size)

    /*println(ngramProbabilities("lol"))
    println(ngramProbabilities("the"))
    println(ngramProbabilities("in the"))
    println(ngramProbabilities("house"))*/

    print("Serializing to " + args(1) + " ...");
    val gos = new GZIPOutputStream(new FileOutputStream(args(1)))
    val oos = new ObjectOutputStream(gos)
    oos.writeObject(probLex/*ngramProbabilities/*Pruned*/*/)
    oos.close()
    println("done.")
  }

  def processDir(dir: File): Unit = {
    for(file <- dir.listFiles) {
      if(!file.isDirectory)
        processFile(file)
      if(tweetCount >= tweetsToProcess)
        return
    }
  }

  def processFile(file: File): Unit = {
    println(file)
    
    val fileInputStream = new FileInputStream(file)
    fileInputStream.read(); // otherwise null pointer
    fileInputStream.read();
    val cbzip2InputStream = new CBZip2InputStream(fileInputStream)
    val in = new BufferedReader(new InputStreamReader(cbzip2InputStream))

    var line = in.readLine
    while(line != null) {
      try {
        //val jsonMap:Map[String, Any] = (Map[String, Any]) JSON.parseFull(line)
        //val tweet = jsonMap("text")
        val tokener = new JSONTokener(line)
        val jsonObject = new JSONObject(tokener)

        val tweet = jsonObject.getString("text")
        tweetCount += 1

        val tokens = tweet.split(" ")
        if(tokens.length >= 1) {
          val unigrams = tokens.map(StringUtil.preprocessKeepHash(_)).toList
          //wordCount += unigrams.length
          val bigramsFromUnigrams =
            if(unigrams.length >= 2)
              unigrams.sliding(2).map(bi => bi(0)+" "+bi(1)).toList
            else
              Nil
          val bigrams = "$ "+unigrams(0) :: (bigramsFromUnigrams ::: (unigrams(unigrams.length-1)+" $" :: Nil))

          for(ngram <- unigrams ::: bigrams) {
            //println("adding: " + ngram)
            probLex.observeNgram(ngram)
          }
        }

        if(tweetCount >= tweetsToProcess)
          return
      }
      catch {
        case e: Exception => 
      }

      line = in.readLine
    }

    in.close
  }
}
