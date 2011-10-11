package updown.data

object SentimentLabel extends Enumeration{
  type Type = Value
  val Positive2 = Value("2")
  val Positive = Value("1")
  val Neutral = Value("0")
  val Negative = Value("-1")
  val Negative2 = Value("-2")
  val Abstained = Value("A")
  // this is the end of the enum definition. the rest of this object just demonstrates other
  //  stuff you can do.

  private val _POS_NAME = "positive"
  private val _POS_NAME2 = "superPositive"
  private val _NEG_NAME = "negative"
  private val _NEG_NAME2 = "superNegative"
  private val _NEU_NAME = "neutral"
  private val _ABS_NAME = "abstained"

  def unitSentiment(label:SentimentLabel.Type) = {
    label match {
      case Positive2 => Positive
      case Negative2 => Negative
      case x  => x
    }
  }

  def ordinality(label:SentimentLabel.Type) = {
    label match {
      case Abstained => 0
      case Negative2 => 1
      case Negative => 2
      case Neutral => 3
      case Positive => 4
      case Positive2 => 5
    }
  }

  def toEnglishName(label:SentimentLabel.Type) = {
    label match {
      case Positive => _POS_NAME
      case Positive2 => _POS_NAME2
      case Negative => _NEG_NAME
      case Negative2 => _NEG_NAME2
      case Neutral  => _NEU_NAME
      case Abstained  => _ABS_NAME
    }
  }

  def fromEnglishName(name:String) = {
    name match {
      case `_POS_NAME` => Positive
      case `_POS_NAME2` => Positive2
      case `_NEG_NAME` => Negative
      case `_NEG_NAME2` => Negative2
      case `_NEU_NAME` => Neutral
      case `_ABS_NAME` => Abstained
    }
  }

  def figureItOut(name:String) = {
    name.toLowerCase match {
      case `_POS_NAME` |"pos"|"p"|"+"|"1" => Positive
      case `_POS_NAME2` |"pos2"|"2" => Positive2
      case `_NEG_NAME`|"neg"|"-"|"-1" => Negative
      case `_NEG_NAME2`|"neg2"|"-2" => Negative2
      case `_NEU_NAME`|"neu"|"neut"|"0" => Neutral
      case `_ABS_NAME` => Abstained
    }
  }
}