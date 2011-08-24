package updown.preproc

import updown.util._
import java.io._

object PreprocEmoticonTweets {

  val topNUnigrams = new scala.collection.mutable.HashMap[String, Int] { override def default(s: String) = 0 }
  val TOP_N = 1000
  
  // TODO: refactor this into a seperate enum
  val NUM_POS = "1"
  val NUM_NEU = "0"
  val NUM_NEG = "-1"

  def main(args: Array[String]) = {
    val out = new OutputStreamWriter(new FileOutputStream(args(3)),"UTF-8")
    val stoplist = scala.io.Source.fromFile(args(4),"utf-8").getLines.toSet
    val engDict = scala.io.Source.fromFile(args(5),"utf-8").getLines.toSet
    val countTopN = args.length >= 6
    preprocFile(args(0), NUM_POS, out, stoplist, engDict, countTopN) //happy
    preprocFile(args(1), NUM_NEG, out, stoplist, engDict, countTopN) //sad
    //it seems i just need to add one little command here:
    preprocFile(args(2), NUM_NEU, out, stoplist, engDict, countTopN) //neutral
    out.close

    if(countTopN) {
      val topNOut = new BufferedWriter(new FileWriter(args(6)))
      
      val topNSorted = topNUnigrams.toList.filter(_._1.length >= 2).sortWith((x, y) => x._2 >= y._2).slice(0, TOP_N)

      topNSorted.foreach(p => topNOut.write(p._1+" "+p._2+"\n"))

      topNOut.close
    }
  }

  def preprocFile(inFilename: String, label: String, out: OutputStreamWriter, stoplist: Set[String], engDict: Set[String], countTopN: Boolean) = {
    for(line <- scala.io.Source.fromFile(inFilename,"utf-8").getLines) {
      val tokens = BasicTokenizer(line)//TwokenizeWrapper(line)
      if(isEnglish(tokens, engDict)) {
        val bigrams = StringUtil.generateBigrams(tokens)
        
        val unigrams = tokens.filterNot(stoplist(_))
        if(countTopN) unigrams.foreach(u => topNUnigrams.put(u, topNUnigrams(u)+1))
        val features = unigrams ::: bigrams

        for(feature <- features) out.write(feature+",")
        out.write(label+"\n")
      }
      if(isArabic(tokens, engDict)) {
        val bigrams = StringUtil.generateBigrams(tokens)
        
        val unigrams = tokens.filterNot(stoplist(_))
        if(countTopN) unigrams.foreach(u => topNUnigrams.put(u, topNUnigrams(u)+1))
        val features = unigrams ::: bigrams

        for(feature <- features) out.write(feature+",")
        out.write(label+"\n")
      }
    }
  }

  def isEnglish(tokens: List[String], engDict: Set[String]): Boolean = {
    val numEnglishTokens = tokens.count(t => t.length >= 2 && engDict(t))
    if(numEnglishTokens >= 2) true else false
  }
  def isArabic(tokens: List[String], araDict: Set[String]): Boolean = {
    val numArabicTokens = tokens.count(t => t.length >= 1 && araDict(t))
    if (numArabicTokens >= 1) true else false
  }
}
