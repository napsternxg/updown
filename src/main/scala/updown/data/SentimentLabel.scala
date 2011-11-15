package updown.data

object SentimentLabel extends Enumeration {
  type Type = Value
  val Positive2 = Value("2")
  val Positive = Value("1")
  val Neutral = Value("0")
  val Negative = Value("-1")
  val Negative2 = Value("-2")
  val Abstained = Value("A")
  val Unknown = Value("U")
  // this is the end of the enum definition. the rest of this object just demonstrates other
  //  stuff you can do.

  private val _POS_NAME = "positive"
  private val _POS_DOUBLE = 1.0
  private val _POS_NAME2 = "superPositive"
  private val _POS_DOUBLE2 = 2.0
  private val _NEG_NAME = "negative"
  private val _NEG_DOUBLE = -1.0
  private val _NEG_NAME2 = "superNegative"
  private val _NEG_DOUBLE2 = -2.0
  private val _NEU_NAME = "neutral"
  private val _NEU_DOUBLE = 0.0
  private val _ABS_NAME = "abstained"
  private val _ABS_DOUBLE = Double.NegativeInfinity // somewhat nonsense; we just need a flag value
  private val _UNK_NAME = "unknown"
  private val _UNK_DOUBLE = Double.NaN

  def unitSentiment(label: SentimentLabel.Type) = {
    label match {
      case Positive2 => Positive
      case Negative2 => Negative
      case x => x
    }
  }

  def ordinality(label: SentimentLabel.Type) = {
    label match {
      case Abstained => 0
      case Negative2 => 1
      case Negative => 2
      case Neutral => 3
      case Positive => 4
      case Positive2 => 5
    }
  }

  def toEnglishName(label: SentimentLabel.Type) = {
    label match {
      case Positive => _POS_NAME
      case Positive2 => _POS_NAME2
      case Negative => _NEG_NAME
      case Negative2 => _NEG_NAME2
      case Neutral => _NEU_NAME
      case Abstained => _ABS_NAME
      case Unknown => _UNK_NAME
    }
  }

  def fromEnglishName(name: String) = {
    name match {
      case `_POS_NAME` => Positive
      case `_POS_NAME2` => Positive2
      case `_NEG_NAME` => Negative
      case `_NEG_NAME2` => Negative2
      case `_NEU_NAME` => Neutral
      case `_ABS_NAME` => Abstained
      case `_UNK_NAME` => Unknown
    }
  }

  def toDouble(label: SentimentLabel.Type) = {
    label match {
      case Positive => _POS_DOUBLE
      case Positive2 => _POS_DOUBLE2
      case Negative => _NEG_DOUBLE
      case Negative2 => _NEG_DOUBLE2
      case Neutral => _NEU_DOUBLE
      case Abstained => _ABS_DOUBLE
      case Unknown => _UNK_DOUBLE
    }
  }

  def fromDouble(name: Double) = {
    name match {
      case `_POS_DOUBLE` => Positive
      case `_POS_DOUBLE2` => Positive2
      case `_NEG_DOUBLE` => Negative
      case `_NEG_DOUBLE2` => Negative2
      case `_NEU_DOUBLE` => Neutral
      case `_ABS_DOUBLE` => Abstained
      case `_UNK_DOUBLE` => Unknown
    }
  }

  def figureItOut(name: String) = {
    try {
      val posDouble = _POS_DOUBLE.toString
      val negDouble = _NEG_DOUBLE.toString
      val neuDouble = _NEU_DOUBLE.toString
      val posDouble2 = _POS_DOUBLE2.toString
      val negDouble2 = _NEG_DOUBLE2.toString
      val absDouble = _ABS_DOUBLE.toString
      val unkDouble = _UNK_DOUBLE.toString
      name.toLowerCase match {
        case `_POS_NAME` | `posDouble` | "pos" | "p" | "+" | "1" => Positive
        case `_POS_NAME2` | `posDouble2` | "pos2" | "2" => Positive2
        case `_NEG_NAME` | `negDouble` | "neg" | "-" | "-1" => Negative
        case `_NEG_NAME2` | `negDouble2` | "neg2" | "-2" => Negative2
        case `_NEU_NAME` | `neuDouble` | "neu" | "neut" | "0" => Neutral
        case `_ABS_NAME` | `absDouble` => Abstained
        case `_UNK_NAME` | `unkDouble` => Unknown
      }
    } catch {
      case e: scala.MatchError =>
        System.err.println("couldn't figure out: \"%s\"".format(name))
        throw e
    }
  }
}