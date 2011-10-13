package updown.preproc

import updown.data.SentimentLabel
import org.clapper.argot.ArgotConverters._
/**
 * This preprocessor is suitable for any file that contains one instance per line with no labels or ids. This variation will concatenate all lines in each file and create just one instance per file.
 */
object PreprocFlatFilesCat extends GenericPreprocessor {
  val minDocSizeOption = parser.option[Int](List("minDocSize"), "INT", "concatenate inputs until the docsize (in characters) reaches INT")

  def getInstanceIterator(fileName: String, polarity: String): Iterator[(String, String, SentimentLabel.Type, String)] = {
    try {
      val minDocSize =
      (if (minDocSizeOption.value.isDefined)
        minDocSizeOption.value.get
      else
        20000)
      var totalLength = 0
      var fileLines = ""
      var result = List[(String, String, SentimentLabel.Type, String)]()
      val source = scala.io.Source.fromFile(fileName, "UTF-8").getLines
      for ((line, index) <- source.zipWithIndex) {
        fileLines += line.replace("|", "")
        if (fileLines.length > minDocSize) {
          result = (fileName + index, "reviewer", SentimentLabel.figureItOut(polarity), fileLines) :: result
          logger.info("processed %d inputs.".format(index))
          totalLength += fileLines.length
          fileLines = ""
        }
      }
      if (fileLines!=""){
        result = (fileName + "Remainder", "reviewer", SentimentLabel.figureItOut(polarity), fileLines) :: result
        totalLength += fileLines.length
      }
      logger.info("average length: %f".format(totalLength.toFloat / result.length))
      result.iterator
    } catch {
      case e: MatchError =>
        logger.error("Couldn't figure out what sentiment '%s' is supposed to be." +
          " Try using 'pos', 'neg', or 'neu'. Skipping %s...".format(polarity, fileName))
        Iterator[(String, String, SentimentLabel.Type, String)]()
    }
  }
}