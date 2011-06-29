package updown.lex

class MPQALexicon(entries: scala.collection.mutable.HashMap[String, MPQAEntry]) {
  def apply(s:String) = entries(s)
  def contains(s:String) = entries.contains(s)
  val keySet = entries.keySet
}

object MPQALexicon {

  val STRONG = "strong"
  val WEAK = "weak"
  val POS = "POS"
  val NEG = "NEG"

  val mpqaLineRE = """^.*word1=(\w+).*mpqapolarity=(strong|weak)(pos|neg).*$""".r

  def apply(filename: String) = {
    val entries = new scala.collection.mutable.HashMap[String, MPQAEntry]

    for(line <- scala.io.Source.fromFile(filename).getLines) {
      try {
        val mpqaLineRE(word, subjectivity, polarity) = line
        entries.put(word, new MPQAEntry(word, polarity.toUpperCase, subjectivity))
      }
      catch { case e: MatchError => }
      
    }

    new MPQALexicon(entries)
  }
}

class MPQAEntry(val word:String, val polarity:String, val subjectivity:String) {
  val oppositePolarity = if(polarity == MPQALexicon.POS) MPQALexicon.NEG else MPQALexicon.POS
  val isPositive = polarity == MPQALexicon.POS
  val isNegative = polarity == MPQALexicon.NEG
  val isStrong = subjectivity == MPQALexicon.STRONG
  val isWeak = subjectivity == MPQALexicon.WEAK

  override def toString = {
    word + ": " + subjectivity + " " + polarity
  }
}
