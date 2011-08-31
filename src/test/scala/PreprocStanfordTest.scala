import org.scalatest.FlatSpec
import updown.data.SentimentLabel
import updown.preproc.{SuccessfulParse, PreprocStanfordTweets}

class PreprocStanfordTest extends FlatSpec {
  val STANFORD_INPUT_LINE = "4;;3;;Mon May 11 03:17:40 UTC 2009;;kindle2;;tpryan;;@stellargirl I loooooooovvvvvveee my Kindle2. Not that the DX is cool, but the 2 is fantastic in its own right."
  val STANFORD_SENTIMENT_RAW = "4"
  val STANFORD_SENTIMENT_GOLD = SentimentLabel.Positive
  val STANFORD_TWEET_ID = "3"
  val STANFORD_USERNAME = "tpryan"
  val STANFORD_TWEET = "@stellargirl I loooooooovvvvvveee my Kindle2. Not that the DX is cool, but the 2 is fantastic in its own right."
  val STANFORD_TOKENS = List("stellargirl", "i", "loooooooovvvvvveee", "my", "kindle2", "not", "that", "the", "dx", "is", "cool", "but", "the", "2", "is", "fantastic", "in", "its", "own", "right")
  val STANFORD_FEATURES = List("stellargirl", "i", "loooooooovvvvvveee", "my", "kindle2", "not", "that", "the", "dx", "is", "cool", "but", "the", "2", "is", "fantastic", "in", "its", "own", "right", "$ stellargirl", "stellargirl i", "i loooooooovvvvvveee", "loooooooovvvvvveee my", "my kindle2", "kindle2 not", "not that", "that the", "the dx", "dx is", "is cool", "cool but", "but the", "the 2", "2 is", "is fantastic", "fantastic in", "in its", "its own", "own right", "right $")

  val pst = PreprocStanfordTweets

  "lineRE" should "parse a test line correctly" in {
    val pst.lineRE(sentimentRaw, tweetid, username, tweet) = STANFORD_INPUT_LINE
    assert(sentimentRaw === STANFORD_SENTIMENT_RAW)
    assert(tweetid === STANFORD_TWEET_ID)
    assert(username === STANFORD_USERNAME)
    assert(tweet === STANFORD_TWEET)
  }

  "processOneLine" should "produce expected output" in {
    assert(
      pst.processOneLine(STANFORD_INPUT_LINE, Set())
        ===
        SuccessfulParse(
          STANFORD_TWEET_ID,
          STANFORD_USERNAME,
          STANFORD_SENTIMENT_GOLD,
          STANFORD_FEATURES))
  }
}