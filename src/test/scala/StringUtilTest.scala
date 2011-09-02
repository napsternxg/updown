import org.scalatest.FlatSpec
import updown.util.StringUtil

class StringUtilTest extends FlatSpec {
  "stripPunc" should "turn /.,@$#asdf';.@#$% into asdf" in {
    assert(StringUtil.stripPunc("/.,@$#ASdf';.@#$%") === "ASdf")
  }
  "preprocess" should "turn /.,@$#ASdf';.@#$% into asdf" in {
    assert(StringUtil.preprocess("/.,@$#ASdf';.@#$%") === "asdf")
  }
  it should "turn /.,@$#;.@#$% into /.,@$#';.@#$%" in {
    assert(StringUtil.preprocess("/.,@$#';.@#$%") === "/.,@$#';.@#$%")
  }
  "stripPuncKeepHash" should "turn /.,@$#asdf';.@#$% into #asdf" in {
    assert(StringUtil.stripPuncKeepHash("/.,@$#asdf';.@#$%") === "#asdf")
  }
  "preprocessKeepHash" should "turn /.,@$#ASdf';.@#$% into #asdf" in {
    assert(StringUtil.preprocessKeepHash("/.,@$#ASdf';.@#$%") === "#asdf")
  }
  it should "turn /.,@$#;.@#$% into /.,@$#';.@#$%" in {
    assert(StringUtil.preprocessKeepHash("/.,@$#';.@#$%") === "/.,@$#';.@#$%")
  }
  "generateBigrams" should "return bigrams for a list of unigrams" in {
    assert(StringUtil.generateBigrams(List("a", "b", "c")) === List("$ a", "a b", "b c", "c $"))
  }
  it should "return Nil for an empty list" in {
    assert(StringUtil.generateBigrams(List()) === Nil)
  }
  it should "return bigrams for an list length 1" in {
    assert(StringUtil.generateBigrams(List("a")) === List("$ a", "a $"))
  }
  it should "return bigrams for an list length 2" in {
    assert(StringUtil.generateBigrams(List("a", "b")) === List("$ a", "a b", "b $"))
  }
}