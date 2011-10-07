package updown.data

object SentimentLabel extends Enumeration{
  type Type = Value
  val Positive = Value("1")
  val Neutral = Value("0")
  val Negative = Value("-1")
  val Abstained = Value("A")
  // this is the end of the enum definition. the rest of this object just demonstrates other
  //  stuff you can do.

  private val _POS_NAME = "positive"
  private val _NEG_NAME = "negative"
  private val _NEU_NAME = "neutral"
  private val _ABS_NAME = "abstained"

  def toEnglishName(label:SentimentLabel.Type) = {
    label match {
      case Positive => _POS_NAME
      case Negative => _NEG_NAME
      case Neutral  => _NEU_NAME
      case Abstained  => _ABS_NAME
    }
  }

  def fromEnglishName(name:String) = {
    name match {
      case `_POS_NAME` => Positive
      case `_NEG_NAME` => Negative
      case `_NEU_NAME` => Neutral
      case `_ABS_NAME` => Abstained
    }
  }

  def figureItOut(name:String) = {
    name.toLowerCase match {
      case `_POS_NAME` |"pos"|"p"|"+"|"1" => Positive
      case `_NEG_NAME`|"neg"|"-"|"-1" => Negative
      case `_NEU_NAME`|"neu"|"neut"|"0" => Neutral
      case `_ABS_NAME` => Abstained
    }
  }
}