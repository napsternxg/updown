package updown.util

object TwokenizeWrapper {
  def apply(s: String): List[String] = {
    Twokenize(s).filterNot(_.contains(",")).map(_.toLowerCase)
  }
}
