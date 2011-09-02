import org.scalatest.FlatSpec
import updown.data.SentimentLabel
import updown.preproc.{SuccessfulHCRParse, PreprocHCRTweets}

class PreprocHCRTest extends FlatSpec {
  val HCR_INPUT_FIELDS = Array("9932982701", "29136568", "Hexham67", "Bully for you Mr. President. Bully for you. #hcr",
    "positive", "obama", "mteisberg", "Could be a compliment, or sarcasm")
  val HCR_SENTIMENT_GOLD = SentimentLabel.Positive
  val HCR_TARGET = "obama"
  val HCR_TWEET_ID = "9932982701"
  val HCR_USERNAME = "Hexham67"
  val HCR_TWEET = "Bully for you Mr. President. Bully for you. #hcr"
  val HCR_TOKENS = List("stellargirl", "i", "loooooooovvvvvveee", "my", "kindle2", "not", "that", "the", "dx", "is", "cool", "but", "the", "2", "is", "fantastic", "in", "its", "own", "right")
  val HCR_FEATURES = List("bully", "president", "bully", "#hcr", "$ bully", "bully for", "for you", "you mr", "mr president", "president bully", "bully for", "for you", "you #hcr", "#hcr $")

  val pst = PreprocHCRTweets

  "processOneLine" should "produce expected output" in {
    assert(
      pst.processOneLine(9, HCR_INPUT_FIELDS, Set("for", "you", "mr"))
        ===
        SuccessfulHCRParse(
          HCR_TWEET_ID,
          HCR_USERNAME,
          List((HCR_SENTIMENT_GOLD,
          HCR_TARGET)),
          HCR_FEATURES))
  }

  //TODO test failure modes
}
