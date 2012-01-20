package updown.data.io

import io.Source
import updown.data.{SentimentLabel, GoldLabeledTweet, Tweet}
import java.io.{FileWriter, OutputStreamWriter, File}

object MDSDFormat extends Format {
  private val STRING_ENC = "UTF8"

  def read(inputFile: File) = {
    var lineNumber = 0
    Source.fromFile(STRING_ENC).getLines().map(
      (line) => {
        val (labelHash :: lineSplit) = line.split("\\s+").toList.reverse
        val words = (for (tokenNCount <- lineSplit) yield {
          val (token :: count :: _) = tokenNCount.split(":").toList
          (for (i <- 0 until Integer.valueOf(count)) yield token).iterator
        }).iterator.flatten
        val Array(_, label) = labelHash.split(":")
        GoldLabeledTweet(inputFile.getName + lineNumber, "?", words.toList, SentimentLabel.figureItOut(label))
      }
    )
  }

  private val count: ((List[String], Map[String, Int]) => Map[String, Int]) =
    (wordList, map) => {
      wordList match {
        case w :: ws => count(ws, map + ((w, map.getOrElse(w, 0) + 1)))
        case _ => map
      }
    }

  private def stringify_counts(counts: Map[String, Int]) = {
    counts.map {
      case (s, i) => "%s:%d".format(s, i)
    }.mkString(" ")
  }

  def write(outputFile: File, instances: Iterator[GoldLabeledTweet]) {
    val out = new FileWriter(outputFile)
    for (GoldLabeledTweet(id, uid, features, label) <- instances) {
      out.write(
        stringify_counts(count(features, Map()))
          + " #label#:%s".format(SentimentLabel.toEnglishName(label))
          + "\n"
      )
    }
    out.close()
  }
}
