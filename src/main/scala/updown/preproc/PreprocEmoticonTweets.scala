package updown.preproc

import updown.util._
import java.io._

object PreprocEmoticonTweets {

  def main(args: Array[String]) = {
    val out = new BufferedWriter(new FileWriter(args(2)))
    val stoplist = scala.io.Source.fromFile(args(3)).getLines.toSet
    val engDict = scala.io.Source.fromFile(args(4)).getLines.toSet
    preprocFile(args(0), "1", out, stoplist, engDict) //happy
    preprocFile(args(1), "-1", out, stoplist, engDict) //sad
    out.close
  }

  def preprocFile(inFilename: String, label: String, out: BufferedWriter, stoplist: Set[String], engDict: Set[String]) = {
    for(line <- scala.io.Source.fromFile(inFilename).getLines) {
      val tokens = TwokenizeWrapper(line)
      if(isEnglish(tokens, engDict)) {
        val bigrams = StringUtil.generateBigrams(tokens)
        
        val unigrams = tokens.filterNot(stoplist(_))
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
}
