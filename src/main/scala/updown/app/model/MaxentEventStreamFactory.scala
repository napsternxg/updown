package updown.app.model

import opennlp.maxent.{DataStream, BasicEventStream}
import updown.data.io.TweetFeatureReader
import opennlp.model.EventStream
import updown.data._

object MaxentEventStreamFactory {
  val DEFAULT_DELIMITER = ","

  def apply(fileName: String): EventStream = {
    getWithStringIterator(scala.io.Source.fromFile(fileName).getLines())
  }

  def getWithStringIterator(iterator: Iterator[String]): EventStream = {
    new BasicEventStream(new DataStream {
      def nextToken(): AnyRef = {
        val GoldLabeledTweet(_, _, features, label) = TweetFeatureReader.parseLine(iterator.next())
        (features ::: (label :: Nil)).mkString(DEFAULT_DELIMITER)
      }

      def hasNext: Boolean = iterator.hasNext
    }, DEFAULT_DELIMITER)
  }

  def getWithGoldLabeledTweetIterator(iterator: Iterator[GoldLabeledTweet]): EventStream = {
    new BasicEventStream(new DataStream {
      def nextToken(): AnyRef = {
        val GoldLabeledTweet(_, _, features, label) = iterator.next()
        (features ::: (label :: Nil)).mkString(DEFAULT_DELIMITER)
      }

      def hasNext: Boolean =
        iterator.hasNext

    }, DEFAULT_DELIMITER)
  }
}