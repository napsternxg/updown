import org.scalatest.FlatSpec
import updown.data.SentimentLabel
import updown.preproc.{SuccessfulHCRParse, PreprocHCRTweets}

class PreprocHCRTest extends FlatSpec {
  val HCR_INPUT_LINE = "4;;3;;Mon May 11 03:17:40 UTC 2009;;kindle2;;tpryan;;@stellargirl I loooooooovvvvvveee my Kindle2. Not that the DX is cool, but the 2 is fantastic in its own right."
  val HCR_SENTIMENT_RAW = "4"
  val HCR_SENTIMENT_GOLD = SentimentLabel.Positive
  val HCR_TWEET_ID = "3"
  val HCR_USERNAME = "tpryan"
  val HCR_TWEET = "@stellargirl I loooooooovvvvvveee my Kindle2. Not that the DX is cool, but the 2 is fantastic in its own right."
  val HCR_TOKENS = List("stellargirl", "i", "loooooooovvvvvveee", "my", "kindle2", "not", "that", "the", "dx", "is", "cool", "but", "the", "2", "is", "fantastic", "in", "its", "own", "right")
  val HCR_FEATURES = List("stellargirl", "i", "loooooooovvvvvveee", "my", "kindle2", "not", "that", "the", "dx", "is", "cool", "but", "the", "2", "is", "fantastic", "in", "its", "own", "right", "$ stellargirl", "stellargirl i", "i loooooooovvvvvveee", "loooooooovvvvvveee my", "my kindle2", "kindle2 not", "not that", "that the", "the dx", "dx is", "is cool", "cool but", "but the", "the 2", "2 is", "is fantastic", "fantastic in", "in its", "its own", "own right", "right $")

  val pst = PreprocHCRTweets

  "lineRE" should "parse a test line correctly" in {
    val pst.lineRE(sentimentRaw, tweetid, username, tweet) = HCR_INPUT_LINE
    assert(sentimentRaw === HCR_SENTIMENT_RAW)
    assert(tweetid === HCR_TWEET_ID)
    assert(username === HCR_USERNAME)
    assert(tweet === HCR_TWEET)
  }

  "processOneLine" should "produce expected output" in {
    assert(
      pst.processOneLine(HCR_INPUT_LINE, Set())
        ===
        SuccessfulHCRParse(
          HCR_TWEET_ID,
          HCR_USERNAME,
          HCR_SENTIMENT_GOLD,
          HCR_FEATURES))
  }
}