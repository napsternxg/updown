import org.scalatest.FlatSpec
import updown.util.BasicTokenizer

class BasicTokenizerTest extends FlatSpec {
  val STANFORD_TWEET = "@stellargirl I loooooooovvvvvveee my Kindle2. Not that the DX is cool, but the 2 is fantastic in its own right."
  val STANFORD_TOKENS = List("stellargirl", "i", "loooooooovvvvvveee", "my", "kindle2", "not", "that", "the", "dx", "is", "cool", "but", "the", "2", "is", "fantastic", "in", "its", "own", "right")
  "BasicTokenizer" should "produce expected output" in {
    assert(BasicTokenizer(STANFORD_TWEET) === STANFORD_TOKENS)
  }
}