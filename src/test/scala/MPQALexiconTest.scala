import collection.mutable.HashMap
import org.scalatest.FlatSpec
import updown.lex.{MPQAEntry, MPQALexicon}

class MPQALexiconTest extends FlatSpec {
  val line = "type=weaksubj len=1 word1=abandoned pos1=adj stemmed1=n polarity=negative polannsrc=ph mpqapolarity=strongneg"
  val map = new HashMap[String, MPQAEntry]()
  MPQALexicon.parseLine("type=weaksubj len=1 word1=abandoned pos1=adj stemmed1=n polarity=negative polannsrc=ph mpqapolarity=strongneg",
    map)
  "parseLine" should "insert 'abandoned' into the dictionary" in {
    assert(map.contains("abandoned"))
  }
  it should "have inserted the correct value as well" in {
    assert(map.get("abandoned").get === new MPQAEntry("abandoned", "NEG", "strong"))
  }

  val entry = new MPQAEntry("abandoned", "NEG", "strong")
  "a strong NEG MPQAEntry" should "have a POS opposite" in assert(entry.oppositePolarity === "POS")
  it should "not be positive" in assert(! entry.isPositive)
  it should "not be neutral" in assert(! entry.isNeutral)
  it should "be negative" in assert(entry.isNegative)
  it should "be strong" in assert(entry.isStrong)
  it should "not be weak" in assert(!entry.isWeak)

}