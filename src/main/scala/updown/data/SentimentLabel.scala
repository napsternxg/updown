package updown.data

object SentimentLabel extends Enumeration{
  type Type = Value
  val Positive = Value("1")
  val Neutral = Value("0")
  val Negative = Value("-1")
  val Invalid = Value("-2")
  // this is the end of the enum definition. the rest of this object just demonstrates other
  //  stuff you can do.

  private val _POS_NAME = "positive"
  private val _NEG_NAME = "negative"
  private val _NEU_NAME = "neutral"

  def toEnglishName = {
    this match {
      case Positive => _POS_NAME
      case Negative => _NEG_NAME
      case Neutral  => _NEU_NAME
    }
  }

  def fromEnglishName(name:String) = {
    name match {
      case `_POS_NAME` => Positive
      case `_NEG_NAME` => Negative
      case `_NEU_NAME` => Neutral
    }
  }

  def figureItOut(name:String) = {
    name.toLowerCase match {
      case "positive"|"pos"|"p"|"+"|"1" => Positive
      case "negative"|"neg"|"-"|"-1" => Negative
      case "neutral"|"neu"|"neut"|"0" => Neutral
    }
  }
}