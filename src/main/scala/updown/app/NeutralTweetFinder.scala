package updown.app

import updown.util._
import updown.lex._

import java.io._
import org.apache.tools.bzip2._
import java.util.zip._
import org.json._

import scala.collection.JavaConversions._

object NeutralTweetFinder {

  //val url = """^.*(http://)|(.co)|(.org)|(.uk)|(.gov)|(.be).*$""".r

  var numWritten = 0
  val STOPWORD_THRESHOLD = 2
  val MAX_TO_WRITE = 1000000

  def main(args: Array[String]): Unit = {
    val inFile = new File(args(0))
    val out = new BufferedWriter(new FileWriter(args(1)))
    val engDict = scala.io.Source.fromFile(args(2), "utf-8").getLines.toSet
    val stoplist = scala.io.Source.fromFile(args(3), "utf-8").getLines.toSet
    val lexicon = MPQALexicon(args(4))
    
    if(inFile.isDirectory) {
      for(file <- inFile.listFiles) {
        if(!file.isDirectory) {
          processFile(file, out, engDict, stoplist, lexicon)
          out.flush

          if(numWritten >= MAX_TO_WRITE) {
            out.close
            return
          }
        }
      }
    }

    out.close
  }

  def processFile(file: File, out: BufferedWriter, engDict: Set[String], stoplist: Set[String], lexicon: MPQALexicon): Unit = {
    println(file)
    
    val fis = new FileInputStream(file)
    fis.read; fis.read
    val cbzis = new CBZip2InputStream(fis)
    val in = new BufferedReader(new InputStreamReader(cbzis))

    var curLine = in.readLine
    while(curLine != null) {
      try {
        val tokener = new JSONTokener(curLine)
        val jso = new JSONObject(tokener)

        val tweet = jso.getString("text")

        processTweet(tweet, out, engDict, stoplist, lexicon)

      } catch {
        case e: Exception => e.printStackTrace
      }

      curLine = in.readLine
    }

    in.close
  }

  def processTweet(tweet: String, out: BufferedWriter, engDict: Set[String], stoplist: Set[String], lexicon: MPQALexicon): Unit = {
    val tokens = BasicTokenizer(tweet)

    var stopwordCount = 0
    for(token <- tokens) {
      if(!engDict(token)) {
        return
      }

      if(lexicon.contains(token)) {
        val entry = lexicon(token)
        if(entry.isPositive || entry.isNegative)
          return
      }
      

      if(stoplist(token))
        stopwordCount += 1
    }

    if(stopwordCount >= STOPWORD_THRESHOLD) {
      out.write(tweet.replaceAll("\n", " ")+"\n")
      numWritten += 1
    }
  }
}
