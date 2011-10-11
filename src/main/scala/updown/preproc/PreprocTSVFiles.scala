package updown.preproc

import updown.data.SentimentLabel

/**
 * This preprocessor is suitable for any file that contains one instance per line with no labels or ids
 */
object PreprocTSVFiles extends GenericPreprocessor {

  val lineRegex = "(\\S*)\\s*(\\S*)\\s*(.*)".r

  def getInstanceIterator(fileName: String, polarity: String): Iterator[(String, String, SentimentLabel.Type, String)] = {
    try {
      for (line <- scala.io.Source.fromFile(fileName, "UTF-8").getLines) yield {
        val lineRegex(id, label, text) = line
        logger.debug("id:%s label:%s opol:%s pol:%s".format(id, label, polarity, SentimentLabel.figureItOut(polarity)))
        (id, "reviewer", SentimentLabel.figureItOut(polarity), text.replace("|", ""))
      }
    } catch {
      case e: MatchError =>
        logger.error("Couldn't figure out what sentiment '%s' is supposed to be." +
          " Try using 'pos', 'neg', or 'neu'. Skipping %s...".format(polarity, fileName))
        Iterator[(String, String, SentimentLabel.Type, String)]()
    }
  }
}