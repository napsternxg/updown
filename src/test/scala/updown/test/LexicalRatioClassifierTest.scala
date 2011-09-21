package updown.test

import collection.mutable.HashMap
import org.scalatest.FlatSpec
import updown.app.LexicalRatioClassifier
import updown.data.Tweet
import updown.lex.{MPQALexicon, MPQAEntry}

class LexicalRatioClassifierTest extends FlatSpec {
  "classifyTweet" should "be null with (0,0,0)" in assert(LexicalRatioClassifier.classifyTweet(0, 0, 0) === null)
  it should "be null with (10,10,0)" in assert(LexicalRatioClassifier.classifyTweet(10, 10, 0) === null)
  it should "be 0 with (10,10,10)" in assert(LexicalRatioClassifier.classifyTweet(10, 10, 10) === "0")
  it should "be 0 with (11,11,10)" in assert(LexicalRatioClassifier.classifyTweet(11, 11, 10) === "0")
  it should "be 1 with (12,11,10)" in assert(LexicalRatioClassifier.classifyTweet(12, 11, 10) === "1")
  it should "be -1 with (11,12,10)" in assert(LexicalRatioClassifier.classifyTweet(11, 12, 10) === "-1")
  it should "be 0 with (12,11,13)" in assert(LexicalRatioClassifier.classifyTweet(12, 11, 13) === "0")
  it should "be 0 with (11,12,13)" in assert(LexicalRatioClassifier.classifyTweet(11, 12, 13) === "0")


  "classifyTweets" should "assign the correct label to a few tweets" in {
    val features = List("stellargirl", "loooooooovvvvvveee", "kindle2", "dx", "cool", "2", "fantastic", "$ stellargirl", "stellargirl i", "i loooooooovvvvvveee", "loooooooovvvvvveee my", "my kindle2", "kindle2 not", "not that", "that the", "the dx", "dx is", "is cool", "cool but", "but the", "the 2", "2 is", "is fantastic", "fantastic in", "in its", "its own", "own right", "right $");
    val map = new HashMap[String, MPQAEntry]()
    map += "good" -> new MPQAEntry("good", "POS", "strong")
    map += "bad" -> new MPQAEntry("bad", "NEG", "strong")
    map += "fact" -> new MPQAEntry("fact", "NEU", "strong")

    val tweets = List[Tweet](new Tweet("3", "tpryan", "good" :: features, "POS"),
      new Tweet("4", "tpryan", "bad" :: features, "NEG"),
      new Tweet("5", "tpryan", "fact" :: features, "NEU"))
    val lexicon = new MPQALexicon(map)

    LexicalRatioClassifier.classifyTweets(tweets, lexicon)

    for (tweet: Tweet <- tweets) {
      val gl = tweet.goldLabel match {
        case "POS" => "1"
        case "NEG" => "-1"
        case "NEU" => "0"
      }
      assert(gl === tweet.systemLabel)
    }
  }
}