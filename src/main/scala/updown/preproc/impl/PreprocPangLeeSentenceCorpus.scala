package updown.preproc.impl

import updown.data.SentimentLabel
import updown.preproc.GenericPreprocessor

/**
 * This preprocessor is suitable for any file that contains one instance per line with no labels or ids
 */
object PreprocPangLeeSentenceCorpus extends GenericPreprocessor {

  def getInstanceIterator(fileName: String, polarity: String): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {
    try {
      for (line <- scala.io.Source.fromFile(fileName, "ISO-8859-1").getLines) yield {
        ("", "reviewer", Left(SentimentLabel.figureItOut(polarity)), line.replace("|", ""))
      }
    } catch {
      case e: MatchError =>
        logger.error("Couldn't figure out what sentiment '%s' is supposed to be." +
          " Try using 'pos', 'neg', or 'neu'. Skipping %s...".format(polarity, fileName))
        Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)]()
    }
  }
}