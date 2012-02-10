package updown.preproc.impl

import updown.data.SentimentLabel
import updown.preproc.GenericPreprocessor
import java.io.File


object PreprocDPArticles extends GenericPreprocessor {

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
  val PathRE = "(.*)/(CON|PRO)\\d*/([^/]+)$".r
  def getInstanceIterator(file:File): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {
    val path = file.getAbsolutePath
    val PathRE(_,perspective,filename) = path
    val label = perspective match {
      case "CON" => SentimentLabel.Negative
      case "PRO" => SentimentLabel.Positive
    }
    val text = scala.io.Source.fromFile(file,"UTF-8").getLines().mkString(" ")
    Iterator((filename,"unk",Left(label),text))
  }
}