package updown.preproc.impl

import updown.data.SentimentLabel
import java.io.File
import updown.preproc.GenericPreprocessor

/**
 * This preprocessor is suitable for any directory that contains files which should each be mapped to one instance
 * whose polarity is signified by the label given to the directory in the inputOption
 */
object PreprocPangLeePolarityCorpus extends GenericPreprocessor {
  val PathRE = "(.*)/(neg|pos)/([^/]+)$".r

  def getInstanceIterator(file: File): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {
    val dir = file
    assert(dir.isDirectory)
    val PathRE(_, label, fname) = dir.getAbsolutePath
    val polarity = label match {
      case "pos" => SentimentLabel.Positive
      case "neg" => SentimentLabel.Negative
    }
    (for (file: File <- dir.listFiles()) yield
      (file.getName,
        "reviewer",
        Left(polarity),
        scala.io.Source.fromFile(file, "ISO-8859-1").getLines().mkString(" ").replace("|", "")
        )
      ).iterator
  }
}