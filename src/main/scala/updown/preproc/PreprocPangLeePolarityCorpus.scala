package updown.preproc

import org.clapper.argot.{ArgotUsageException, ArgotConverters, ArgotParser}
import ArgotConverters._
import updown.util.Twokenize
import updown.data.SentimentLabel
import java.io.File
import io.Source

/**
 * This preprocessor is suitable for any directory that contains files which should each be mapped to one instance
 * whose polarity is signified by the label given to the directory in the inputOption
 */
object PreprocPangLeePolarityCorpus {
  // this is here to make ArgotConverters appear used to IDEA.
  convertString _

  def getInputIterator(inputOption: Option[String]): Iterator[String] = {
    inputOption match {
      case Some(fileNameList) =>
        (for ((name, polarity) <- fileNameList.split("\\s*,\\s*").map((pair) => {
          val plist = pair.split("\\s*->\\s*")
          (plist(0) -> plist(1))
        }
        ).toMap) yield {
          try {
            val canonicalPol = SentimentLabel.figureItOut(polarity)
            val dir = new File(name)
            assert(dir.isDirectory)
            (for (file <- dir.listFiles()) yield {
              val text: Source = scala.io.Source.fromFile(file, "ISO-8859-1")
              val string = text.getLines().mkString(" ")
              "%s|%s|%s".format(file.getName, canonicalPol, string.replace("|", ""))
            }).iterator
          } catch {
            case e: MatchError =>
              System.err.println("Couldn't figure out what sentiment '%s' is supposed to be." +
                " Try using 'pos', 'neg', or 'neu'. Skipping %s...".format(polarity, name))
              Iterator[String]()
            case e =>
              System.err.println("Caught some error. Skipping " + name)
              e.printStackTrace()
              Iterator[String]()
          }
        }).iterator.flatten

      case None => scala.io.Source.stdin.getLines()
    }
  }

  def main(args: Array[String]) {
    // PARSE ARGS
    val parser = new ArgotParser("updown run updown.preproc.PreprocStanfordTweets", preUsage = Some("Updown"))
    val inputFile = parser.option[String](List("i", "input"), "input", "path to stanford data file")
    val stopListFile = parser.option[String](List("s", "stoplist"), "stoplist", "path to stoplist file")
    val startId = parser.option[Int](List("start-id"), "ID", "id at which to start numbering lines")
    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException =>
        println(e.message)
        sys.exit(0)
    }

    // SET UP IO
    var instanceID =
      startId.value match {
        case Some(id) => id
        case None => 0
      }

    val inputLines: Iterator[String] =
      getInputIterator(inputFile.value)

    val stopSet: Set[String] =
      stopListFile.value match {
        case Some(fileName) =>
          scala.io.Source.fromFile(fileName).getLines.toSet
        case None => Set("a", "the", ".")
      }

    // RUN
    for (line <- inputLines) {
      line.split('|') match {
        case Array(filename, polarity, text) =>
          println(
            "%s|%s|%s|%s".format(
              filename,
              "reviewer",
              Twokenize(text.replaceAll(",", "")).toList.filter((s) => !stopSet.contains(s)).mkString(","),
              polarity)
          )
      }
      instanceID += 1
    }
  }
}