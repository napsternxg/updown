package updown.preproc

import updown.util._
import java.io._

import org.clapper.argot._
import updown.data.SentimentLabel

object PreprocEmoticonTweets {

  import ArgotConverters._

  val topNUnigrams = new scala.collection.mutable.HashMap[String, Int] {
    override def default(s: String) = 0
  }
  val TOP_N = 1000

  val parser = new ArgotParser("updown run updown.preproc.PreprocEmoticonTweets", preUsage = Some("Updown"))
  val inputPositiveFile = parser.option[String](List("p", "positive"), "positive", "text file with positive emoticons")
  val inputNegativeFile = parser.option[String](List("n", "negative"), "negative", "text file with negative emoticons")
  val outputFile = parser.option[String](List("o", "output"), "ouput", "feature file output")
  val stopListFile = parser.option[String](List("l", "stoplist"), "stoplist", "stoplist words")
  val dictFile = parser.option[String](List("d", "dictionary"), "dictionary", "a dictionary-this is actually just a list of words in the language")
  val countArg = parser.option[String](List("c", "count"), "count", "write top N words to this file")

  def main(args: Array[String]) = {
    try {
      parser.parse(args)
    }

    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(0)
    }

    if (inputPositiveFile.value == None) {
      println("You must specify a text file with happy looking emoticons via -p ")
      sys.exit(0)
    }
    if (inputNegativeFile.value == None) {
      println("You must specify a text file with sad looking emoticons via -n ")
      sys.exit(0)
    }

    val out = new OutputStreamWriter(
      (if (outputFile.value == None) {
        System.out
      } else {
        new FileOutputStream(outputFile.value.get)
      }),
      "UTF-8")
    
    if (stopListFile.value == None) {
      println("You must specify a a stoplist via -s ")
      sys.exit(0)
    }
    if (dictFile.value == None) {
      println("You must specify a dictionary file via -d ")
      sys.exit(0)
    }

    val stoplist = scala.io.Source.fromFile(stopListFile.value.get, "utf-8").getLines.toSet
    val engDict = scala.io.Source.fromFile(dictFile.value.get, "utf-8").getLines.toSet
    val countTopN = if (countArg.value == None) false else true

    preprocFile(inputPositiveFile.value.get, SentimentLabel.Positive, out, stoplist, engDict, countTopN) //happy
    preprocFile(inputNegativeFile.value.get, SentimentLabel.Negative, out, stoplist, engDict, countTopN) //sad

    /*
    *
    *I have no idea what a neutral emoticon is
    */
    //preprocFile(args(2), SentimentLabel.Neutral, out, stoplist, engDict, countTopN)

    if (countTopN) {
      val topNOut = new OutputStreamWriter(new FileOutputStream(countArg.value.get), "utf-8")

      val topNSorted = topNUnigrams.toList.filter(_._1.length >= 2).sortWith((x, y) => x._2 >= y._2).slice(0, TOP_N)

      topNSorted.foreach(p => topNOut.write(p._1 + " " + p._2 + "\n"))

      topNOut.close()
    }
    out.close()
  }

  def preprocFile(inFilename: String, label: SentimentLabel.Type, out: OutputStreamWriter, stoplist: Set[String], engDict: Set[String], countTopN: Boolean) = {
    for (line <- scala.io.Source.fromFile(inFilename, "utf-8").getLines) {
      val tokens = BasicTokenizer(line) //TwokenizeWrapper(line)
      if (isEnglish(tokens, engDict)) {
        val bigrams = StringUtil.generateBigrams(tokens)

        val unigrams = tokens.filterNot(stoplist(_))
        if (countTopN) unigrams.foreach(u => topNUnigrams.put(u, topNUnigrams(u) + 1))
        val features = unigrams ::: bigrams

        for (feature <- features) out.write(feature + ",")
        out.write(label + "\n")
      }
      if (isArabic(tokens, engDict)) {
        val bigrams = StringUtil.generateBigrams(tokens)

        val unigrams = tokens.filterNot(stoplist(_))
        if (countTopN) unigrams.foreach(u => topNUnigrams.put(u, topNUnigrams(u) + 1))
        val features = unigrams ::: bigrams

        for (feature <- features) out.write(feature + ",")
        out.write(label + "\n")
      }
    }
  }

  def isEnglish(tokens: List[String], engDict: Set[String]): Boolean = {
    val numEnglishTokens = tokens.count(t => t.length >= 2 && engDict(t))
    if (numEnglishTokens >= 2) true else false
  }

  def isArabic(tokens: List[String], araDict: Set[String]): Boolean = {
    val numArabicTokens = tokens.count(t => t.length >= 1 && araDict(t))
    if (numArabicTokens >= 1) true else false
  }
}
