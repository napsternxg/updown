package updown.preproc.impl

import updown.data.SentimentLabel
import java.io.File
import updown.preproc.GenericPreprocessor

/**
 * This preprocessor is suitable for any directory that contains files which should each be mapped to one instance
 * whose polarity is signified by the label given to the directory in the inputOption
 */
object PreprocPangLeePolarityCorpus extends GenericPreprocessor {

  def getInstanceIterator(fileName: String, polarity: String): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {
    try {
      val dir = new File(fileName)
      assert(dir.isDirectory)
      (for (file: File <- dir.listFiles()) yield
        (file.getName,
          "reviewer",
          Left(SentimentLabel.figureItOut(polarity)),
          scala.io.Source.fromFile(file, "ISO-8859-1").getLines().mkString(" ").replace("|", "")
          )
        ).iterator
    } catch {
      case e: MatchError =>
        logger.error("Couldn't figure out what sentiment '%s' is supposed to be." +
          " Try using 'pos', 'neg', or 'neu'. Skipping %s...".format(polarity, fileName))
        Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)]()
    }
  }
}