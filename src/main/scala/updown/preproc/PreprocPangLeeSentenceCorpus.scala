package updown.preproc

import org.clapper.argot.{ArgotUsageException, ArgotConverters, ArgotParser}
import ArgotConverters._
import updown.util.Twokenize

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
    val inputLines: Iterator[String] =
      inputFile.value match {
        case Some(fileNameList) =>
          val tempArr: Array[String] = fileNameList.split("\\s*,\\s*")
          val tempArrList: Array[(String, String)] = tempArr.map((pair) => {
            val plist = pair.split("\\s*->\\s*")
            (plist(0) -> plist(1))
          }
          )

          val fileNameMap: Map[String, String] = tempArrList.toMap

          Iterator.flatten((
            for ((name, polarity) <- fileNameMap) yield {
              for (line <- scala.io.Source.fromFile(name, "ISO-8859-1").getLines) yield {
                "%s|%s".format(polarity, line.replace("|", ""))
              }
            }).iterator)

        case None => scala.io.Source.stdin.getLines()
      }

    val stopSet: Set[String] =
      stopListFile.value match {
        case Some(fileName) =>
          scala.io.Source.fromFile(fileName).getLines.toSet
        case None => Set("a", "the", ".")
      }

    // RUN
    var x = 0
    for (line <- inputLines) {
      // mostly replacing the delimiter in the input string as documentation that no matter what you are using
      // to tokenize the string, you must strip out the character that you intend to delimit tokens with later.

      line.split('|') match {
        case Array(polarity, text) =>
        println(
        "%s|%s|%s|%s".format(
              x,
              "",
              Twokenize(text.replaceAll(",", "")).toList.filter((s) => !stopSet.contains(s)).mkString(","),
              polarity)
      )
      }
      /*println(
        "%s|%s|%s|%s".format(
              x,
              "",
              Twokenize(text.replaceAll(",", "")).toList.filter((s) => !stopSet.contains(s)).mkString(","),
              polarity)
      )*/
      /*println(
        line match {
          case inputRegex(polarity, text) =>
            "%s|%s|%s|%s".format(
              x,
              "",
              Twokenize(text.replaceAll(",", "")).toList.filter((s) => !stopSet.contains(s)).mkString(","),
              polarity)
          case _ =>
            ""
        }
      )*/
    }
  }
}