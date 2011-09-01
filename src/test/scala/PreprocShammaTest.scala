import org.scalatest.FlatSpec
import updown.data.SentimentLabel
import updown.preproc.{SuccessfulShammaParse, PreprocShammaTweets}

class PreprocShammaTest extends FlatSpec {
  val SHAMMA_INPUT_LINE = "936472030\t9/27/08 1:03\tPreparing to have a heart attack #tweetdebate\tkyeung808\tKen Yeung\t1\t1\t1\t1\t\t\t\t"
  val SHAMMA_SENTIMENT_RAW = "1\t1\t1\t1\t\t\t\t"
  val SHAMMA_SENTIMENT_GOLD = SentimentLabel.Negative
  val SHAMMA_IAA = 1.0
  val SHAMMA_TWEET_ID = "936472030"
  val SHAMMA_USERNAME = "kyeung808"
  val SHAMMA_TWEET = "Preparing to have a heart attack #tweetdebate"
  val SHAMMA_TOKENS = List("preparing", "to", "have", "a", "heart", "attack", "#tweetdebate")
  val SHAMMA_FEATURES = List("preparing", "heart", "attack", "#tweetdebate", "$ preparing", "preparing to", "to have", "have a", "a heart", "heart attack", "attack #tweetdebate", "#tweetdebate $")

  "lineRE" should "parse a test line correctly" in {
    val PreprocShammaTweets.lineRE(tweetid, tweet, username, sentimentRaw) = SHAMMA_INPUT_LINE
    assert(sentimentRaw === SHAMMA_SENTIMENT_RAW)
    assert(tweetid === SHAMMA_TWEET_ID)
    assert(username === SHAMMA_USERNAME)
    assert(tweet === SHAMMA_TWEET)
  }

  "processOneLine" should "produce expected output" in {
    assert(
      PreprocShammaTweets.processOneLine(SHAMMA_INPUT_LINE, Set("to", "have", "a"))
        ===
        SuccessfulShammaParse(
          SHAMMA_TWEET_ID,
          SHAMMA_USERNAME,
          SHAMMA_SENTIMENT_GOLD,
          SHAMMA_IAA,
          SHAMMA_FEATURES))
  }
}