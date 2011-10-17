package updown.util

object BasicTokenizer {

  def apply(s: String): List[String] = tokenize(s)

  def tokenize(s: String): List[String] = {
    s.split("[\\s+]").map(StringUtil.preprocessKeepHash(_)).filter(_.length > 0).toList
  }
}
