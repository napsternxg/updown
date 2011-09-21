package updown.preproc

import org.clapper.argot.{ArgotUsageException, ArgotConverters, ArgotParser}
import ArgotConverters._
import updown.util.{Twokenize, TokenizationPipes}

object PreprocPangLeeSentenceCorpus {
  // this is here to make ArgotConverters appear used to IDEA.
  convertString _

  def main(args: Array[String]) {
    // PARSE ARGS
    val parser = new ArgotParser("updown run updown.preproc.PreprocStanfordTweets", preUsage = Some("Updown"))
    val inputFile = parser.option[String](List("i", "input"), "input", "path to stanford data file")
    val stopListFile = parser.option[String](List("s", "stoplist"), "stoplist", "path to stoplist file")
    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException =>
        println(e.message)
        sys.exit(0)
    }

    // SET UP IO
    val inputLines =
      inputFile.value match {
        case Some(fileName) => scala.io.Source.fromFile(fileName, "ISO-8859-1").getLines()
        case None => scala.io.Source.stdin.getLines()
      }

    val stopSet: Set[String] =
      stopListFile.value match {
        case Some(fileName) => scala.io.Source.fromFile(fileName).getLines.toSet
        case None => Set("a")
      }

    // RUN
    val tokenizationPipeline: List[(List[String]) => List[String]] = List(
      TokenizationPipes.toLowercase,
      TokenizationPipes.splitOnDelimiter("\\s"),
//      TokenizationPipes.addNGrams(2),
      TokenizationPipes.filterOnStopset(stopSet)
    )

    for (line <- inputLines) {
      var resultLine = List(line)
      for (f <- tokenizationPipeline) {
        resultLine = f(resultLine)
      }
      println(line)
      println("-> " + resultLine.mkString("\t"))
      println("=> " + Twokenize(line).mkString("\t"))
    }
  }
}