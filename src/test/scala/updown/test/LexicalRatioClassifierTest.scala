package updown.test

import collection.mutable.HashMap
import org.scalatest.FlatSpec
import updown.app.LexicalRatioClassifier
import updown.lex.{MPQALexicon, MPQAEntry}
import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel, Tweet}

class LexicalRatioClassifierTest extends FlatSpec {
  "classifyTweet" should "be null with (0,0,0)" in assert(LexicalRatioClassifier.classifyTweet(0, 0, 0) === null)
  it should "be null with (10,10,0)" in assert(LexicalRatioClassifier.classifyTweet(10, 10, 0) === null)
  it should "be 0 with (10,10,10)" in assert(LexicalRatioClassifier.classifyTweet(10, 10, 10) === SentimentLabel.Neutral)
  it should "be 0 with (11,11,10)" in assert(LexicalRatioClassifier.classifyTweet(11, 11, 10) === SentimentLabel.Neutral)
  it should "be 1 with (12,11,10)" in assert(LexicalRatioClassifier.classifyTweet(12, 11, 10) === SentimentLabel.Positive)
  it should "be -1 with (11,12,10)" in assert(LexicalRatioClassifier.classifyTweet(11, 12, 10) === SentimentLabel.Negative)
  it should "be 0 with (12,11,13)" in assert(LexicalRatioClassifier.classifyTweet(12, 11, 13) === SentimentLabel.Neutral)
  it should "be 0 with (11,12,13)" in assert(LexicalRatioClassifier.classifyTweet(11, 12, 13) === SentimentLabel.Neutral)


  "classifyTweets" should "assign the correct label to a few tweets" in {
    val features = List("stellargirl", "loooooooovvvvvveee", "kindle2", "dx", "cool", "2", "fantastic", "$ stellargirl", "stellargirl i", "i loooooooovvvvvveee", "loooooooovvvvvveee my", "my kindle2", "kindle2 not", "not that", "that the", "the dx", "dx is", "is cool", "cool but", "but the", "the 2", "2 is", "is fantastic", "fantastic in", "in its", "its own", "own right", "right $");
    val map = new HashMap[String, MPQAEntry]()
    map += "good" -> new MPQAEntry("good", "POS", "strong")
    map += "bad" -> new MPQAEntry("bad", "NEG", "strong")
    map += "fact" -> new MPQAEntry("fact", "NEU", "strong")

    val glTweets = List[Tweet](GoldLabeledTweet("3", "tpryan", "good" :: features, SentimentLabel.Positive),
      GoldLabeledTweet("4", "tpryan", "bad" :: features, SentimentLabel.Negative),
      GoldLabeledTweet("5", "tpryan", "fact" :: features, SentimentLabel.Neutral))
    val lexicon = new MPQALexicon(map)

    val slTweets = LexicalRatioClassifier.classifyTweets(glTweets, lexicon)

    for (tweet: SystemLabeledTweet <- slTweets) {
      assert(tweet.goldLabel === tweet.systemLabel)
    }
  }
}