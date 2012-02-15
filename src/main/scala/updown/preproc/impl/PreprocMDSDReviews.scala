package updown.preproc.impl

import updown.data.SentimentLabel
import updown.preproc.GenericPreprocessor
import au.com.bytecode.opencsv.CSVReader
import java.io.{File, FileInputStream, InputStreamReader}


object PreprocMDSDReviews extends GenericPreprocessor {
  override val defaultPipeline = "basicTokenize"

  override def before() {
    pipeStages = pipeStages + (("filterBigrams", (ss: List[String]) => {
      ss.filterNot(_.contains("_"))
    }))
  }

  val getTokensFromLine: (String) => (List[String], SentimentLabel.Type) = line => {
    lazy val getTokensFromLineHelper: (List[String], List[String], SentimentLabel.Type) => (List[String], SentimentLabel.Type) =
      (inputs, tokens, label) => {
        inputs match {
          case Nil => (tokens, label)
          case s :: ss =>
            val (left :: right :: Nil) = s.split(":").toList
            left match {
              case "#label#" => getTokensFromLineHelper(ss, tokens, SentimentLabel.figureItOut(right))
              case s: String => getTokensFromLineHelper(ss, tokens ::: (for (_ <- 0 until Integer.valueOf(right)) yield left).toList, label)
            }
        }
      }
    getTokensFromLineHelper(line.split("\\s+").toList, Nil, SentimentLabel.Unknown)
  }

  def getInstanceIterator(file:File): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {
    println(file.getAbsolutePath)
    (for ((line, index) <- scala.io.Source.fromFile(file, "UTF-8").getLines().zipWithIndex) yield {
      val (tokens, label) = getTokensFromLine(line)
      val purgedTokens = tokens.filter((s) => true)
      ("%s#%d".format(file.getName, index), "unk", Left(label), purgedTokens.mkString(" "))
    })
  }
}
