package updown.lex

class MPQALexicon(entries: scala.collection.mutable.HashMap[String, MPQAEntry]) {
  def apply(s: String) = entries(s)

  def contains(s: String) = entries.contains(s)

  def peek(s: String) = entries.get(s).get

  val keySet = entries.keySet
}

object MPQALexicon {

  /* some slight modifications to how things are done here...*/
  val STRONG = "strong"
  val WEAK = "weak"
  val POS = "POS"
  val NEG = "NEG"
  val NEU = "NEU"

  //  val mpqaLineRE = """^.*word1=(\w+).*mpqapolarity=(neutral)|(weakneg)|(strongneg)|(weakpos)|(strongpos).*$""".r
  /* broke up one regex into two. Note the below two aint equiv to above one.*/
  val wordRE = """word1=(\w+)""".r
  val polarityRE = """mpqapolarity=(neutral)|(neg)|(pos)$""".r
  val subjectivityRE = """mpqapolarity=(strong)|(weak)$""".r

  def apply(filename: String) = {
    val entries = new scala.collection.mutable.HashMap[String, MPQAEntry]

    for (line <- scala.io.Source.fromFile(filename, "utf-8").getLines) {
      try {
        //whoa! interesting line of code. but causes runtime match error. shouldn't we be passing character seq and not strings?..
        //val mpqaLineRE(word, subjectivity, polarity) = line
        //entries.put(word, new MPQAEntry(word, polarity.toUpperCase, subjectivity))
        var wordOption = wordRE.findFirstIn(line)
        var polarityOption = polarityRE.findFirstIn(line)
        var subjectivityOption = subjectivityRE.findFirstIn(line)

        if (wordOption != None && polarityOption != None && subjectivityOption != None) {
          val word = wordOption.get.substring(6)
          val polarity = polarityOption.get.toUpperCase
          val subjectivity = subjectivityOption.get.split("=").last

          entries.put(word, new MPQAEntry(word, polarity, subjectivity))

        }
      }
      catch {
        case e: MatchError => println("a match error")
      }
    }

    new MPQALexicon(entries)
  }
}


class MPQAEntry(val word: String, val polarity: String, val subjectivity: String) {
  val oppositePolarity = if (polarity == MPQALexicon.POS) MPQALexicon.NEG else MPQALexicon.POS
  /*matt is not sure about keepin this line */
  val isPositive = polarity == MPQALexicon.POS
  val isNegative = polarity == MPQALexicon.NEG
  val isNeutral = polarity == MPQALexicon.NEU
  val isStrong = subjectivity == MPQALexicon.STRONG
  val isWeak = subjectivity == MPQALexicon.WEAK

  override def toString = {
    word + ": " + polarity + ", " + subjectivity
  }
}
