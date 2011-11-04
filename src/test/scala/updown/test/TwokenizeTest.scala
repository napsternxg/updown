package updown.test

import org.scalatest.FlatSpec
import updown.util.Twokenize

class TwokenizeTest extends FlatSpec {
  "Twokenize" should "tokenize a simple tweet" in {
    assert(Twokenize("@stellargirl I loooooooovvvvvveee my Kindle2. Not that the DX is cool, but the 2 is fantastic in its own right.")
      ===
      List("@stellargirl", "I", "loooooooovvvvvveee", "my", "Kindle2", ".", "Not", "that",
        "the", "DX", "is", "cool", ",", "but", "the", "2", "is", "fantastic", "in", "its", "own", "right", "."))
  }
}