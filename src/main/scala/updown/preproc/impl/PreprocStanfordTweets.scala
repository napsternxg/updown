package updown.preproc.impl

import updown.util._

import org.clapper.argot._
import updown.data.SentimentLabel
import updown.preproc.model.TweetParse
import updown.preproc.GenericPreprocessor
import java.io.File

object PreprocStanfordTweets extends GenericPreprocessor {
  //IDEA will try to remove this import, but it is not unused. Make sure it stays here.
  // See http://devnet.jetbrains.com/message/5301770;jsessionid=5C12AD4FD62857DAD611E8EEED52DF6A

  import ArgotConverters._

  val lineRE = """^(\d+);;(\d+);;[^;]+;;[^;]+;;([^;]+);;(.*)$""".r

  val STAN_POS = "4"
  val STAN_NEU = "2"
  val STAN_NEG = "0"

  override val defaultPipeline = "basicTokenize|addBigrams|removeStopwords"
  def getInstanceIterator(file:File): Iterator[(String, String, Either[SentimentLabel.Type, Map[String, SentimentLabel.Type]], String)] = {

    for (line <- scala.io.Source.fromFile(file, "UTF-8").getLines) yield {
      val lineRE(sentimentRaw, id, username, tweet) = line
      val label = sentimentRaw match {
        case STAN_POS => SentimentLabel.Positive
        case STAN_NEU => SentimentLabel.Neutral
        case STAN_NEG => SentimentLabel.Negative
      }
      logger.debug("id:%s label:%s".format(id, label))
      (id, username, Left(label), tweet)
    }
  }
}
