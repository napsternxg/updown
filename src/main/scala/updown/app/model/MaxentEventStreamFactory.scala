package updown.app.model

import opennlp.maxent.{DataStream, BasicEventStream}
import updown.data.io.TweetFeatureReader
import updown.data.{SentimentLabel, Tweet}
import opennlp.model.EventStream

object MaxentEventStreamFactory {
  val DEFAULT_DELIMITER = ","
  def apply(fileName: String): EventStream = {
    apply(scala.io.Source.fromFile(fileName).getLines)
  }

  def apply(iterator: Iterator[String]): EventStream = {
    new BasicEventStream(new DataStream {
      def nextToken(): AnyRef = {
        TweetFeatureReader.parseLine(iterator.next()) match {
          case Tweet(tweetid, userid, features, label, systemLabel) =>
            (features ::: (label::Nil)).mkString(DEFAULT_DELIMITER)
          case _ =>
            throw new RuntimeException("bad line")
        }
      }
      def hasNext: Boolean = iterator.hasNext
    }, DEFAULT_DELIMITER)
  }
}