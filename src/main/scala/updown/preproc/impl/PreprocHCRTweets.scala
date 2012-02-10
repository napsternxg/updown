package updown.preproc.impl

import updown.data.SentimentLabel
import updown.preproc.GenericPreprocessor
import au.com.bytecode.opencsv.CSVReader
import java.io.{File, FileInputStream, InputStreamReader}


object PreprocHCRTweets extends GenericPreprocessor {
  //IDEA will try to remove this import, but it is not unused. Make sure it stays here.
  // See http://devnet.jetbrains.com/message/5301770;jsessionid=5C12AD4FD62857DAD611E8EEED52DF6A

  val lineRE = """^([^,]*),([^,]*),([^,]*),([^,]*),(.*)$""".r

  val HCR_POS = "positive"
  val HCR_NEG = "negative"
  val HCR_NEU = "neutral"

  override val defaultPipeline = "basicTokenize|addBigrams|removeStopwords"

  def getTargetToLabelMap(labelInfo: List[String]): Map[String, SentimentLabel.Type] = {
    labelInfo match {
      case sentiment :: target :: _ :: _ :: _ :: rest =>
        val label =
          sentiment match {
            case `HCR_POS` => SentimentLabel.Positive
            case `HCR_NEG` => SentimentLabel.Negative
            case `HCR_NEU` => SentimentLabel.Neutral
            case _ => SentimentLabel.Abstained
          }
        getTargetToLabelMap(rest) + ((target, label))
      case _ => Nil.toMap
    }
  }

  def getInstanceIterator(file:File): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {
    // Thanks for not giving us an iterator
    val reader = new CSVReader(new InputStreamReader(new FileInputStream(file),"UTF-8"))
    var fields = reader.readNext() // burn the header column
    fields = reader.readNext()
    var lines = List[List[String]]()
    while (fields != null) {
      lines = fields.toList.map((s) => s.trim) :: lines
      fields = reader.readNext()
    }
    lines = lines.reverse

    // now that we have one, we can use it
    (for (fields: List[String] <- lines) yield {
      val (tid :: (uid :: (uname :: (tweet :: labelInfo)))) = fields
      val newLabelInfo =
        (labelInfo.length % 5) match {
          case 0 => labelInfo
          case 1 => labelInfo ::: List("", "", "", "")
          case 2 => labelInfo ::: List("", "", "")
          case 3 => labelInfo ::: List("", "")
          case 4 => labelInfo ::: List("")
        }
      assert(newLabelInfo.length % 5 == 0)
      val targetToLabelMap: Map[String, SentimentLabel.Type] = getTargetToLabelMap(newLabelInfo)
      (tid, uname, Right(targetToLabelMap), tweet)
    }).iterator
  }
}