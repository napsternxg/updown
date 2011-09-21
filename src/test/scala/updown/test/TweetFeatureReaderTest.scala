package updown.test

import org.scalatest.FlatSpec
import updown.data.io.TweetFeatureReader
import updown.data.Tweet

class TweetFeatureReaderTest extends FlatSpec {
  val line = "3|tpryan|stellargirl,loooooooovvvvvveee,kindle2,dx,cool,2,fantastic,$ stellargirl,stellargirl i,i loooooooovvvvvveee,loooooooovvvvvveee my,my kindle2,kindle2 not,not that,that the,the dx,dx is,is cool,cool but,but the,the 2,2 is,is fantastic,fantastic in,in its,its own,own right,right $|1"
  val features = List("stellargirl", "loooooooovvvvvveee", "kindle2", "dx", "cool", "2", "fantastic", "$ stellargirl", "stellargirl i", "i loooooooovvvvvveee", "loooooooovvvvvveee my", "my kindle2", "kindle2 not", "not that", "that the", "the dx", "dx is", "is cool", "cool but", "but the", "the 2", "2 is", "is fantastic", "fantastic in", "in its", "its own", "own right", "right $");

  "featureRowRE" should "work" in {
    val TweetFeatureReader.featureRowRE(tweetid, userid, featureString, label) = line
    assert(tweetid === "3")
    assert(userid === "tpryan")
    assert(featureString === "stellargirl,loooooooovvvvvveee,kindle2,dx,cool,2,fantastic,$ stellargirl,stellargirl i,i loooooooovvvvvveee,loooooooovvvvvveee my,my kindle2,kindle2 not,not that,that the,the dx,dx is,is cool,cool but,but the,the 2,2 is,is fantastic,fantastic in,in its,its own,own right,right $")
    assert(label === "1")
  }

  "parseLine" should "produce the right Tweet obj" in {
    assert(TweetFeatureReader.parseLine(line)
      ===
      new Tweet("3", "tpryan", features, "POS"))
  }

  // not testing standardize because it will be obviated when I refactor the label to use the SentimentLabel enum
}