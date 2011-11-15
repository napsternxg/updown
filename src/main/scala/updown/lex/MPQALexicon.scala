package updown.lex

import collection.mutable.HashMap

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

  //val mpqaLineRE = """^.*word1=(\w+).*mpqapolarity=(neutral)|(weakneg)|(strongneg)|(weakpos)|(strongpos).*$""".r
  /* broke up one regex into two. Note the below two aint equiv to above one.*/
  val wordRE = """word1=(\w+)""".r
  val polarityRE = """mpqapolarity=\w*(neutral|neg|pos)""".r
  val subjectivityRE = """mpqapolarity=\w*(strong|weak)""".r

  def parseLine(line: String, entries: HashMap[String, MPQAEntry]): Any = {
    //whoa! interesting line of code. but causes runtime match error. shouldn't we be passing character seq and not strings?..
    //val mpqaLineRE(word, subjectivity, polarity) = line
    //entries.put(word, new MPQAEntry(word, polarity.toUpperCase, subjectivity))
    val wordOption = wordRE.findFirstIn(line)
    val polarityOption = polarityRE.findFirstIn(line)
    val subjectivityOption = subjectivityRE.findFirstIn(line)

    if (wordOption != None && polarityOption != None/* && subjectivityOption != None*/) {
      val word = wordOption.get.substring(6)
      val rawPolarity = polarityOption.get.toUpperCase.split("=").last
      val polarity = if(rawPolarity.endsWith(POS)) POS
                     else if(rawPolarity.endsWith(NEG)) NEG
                     else NEU
      val subjectivity = if(subjectivityOption != None) subjectivityOption.get.split("=").last else NEU

      entries.put(word, new MPQAEntry(word, polarity, subjectivity))
    }
  }

  def apply(filename: String) = {
    val entries = new HashMap[String, MPQAEntry]

    for (line <- scala.io.Source.fromFile(filename, "utf-8").getLines) {
      try {
        parseLine(line, entries)
      }
      catch {
        case e: MatchError => println("malformed line: " + line)
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

  override def equals(right: Any):Boolean = {
    if (right != null && right.isInstanceOf[MPQAEntry]){
      val rightEntry = right.asInstanceOf[MPQAEntry]
      (this.word == rightEntry.word) &&
      (this.polarity == rightEntry .polarity) &&
      (this.subjectivity == rightEntry.subjectivity)
    } else {
      false
    }
  }
}

object MPQALexiconTest {
  def main(args: Array[String]) = {
    val lexicon = MPQALexicon(args(0))
    println("Number of entries: "+lexicon.keySet.size)
    println(lexicon("great"))
    println(lexicon("glee"))
    println(lexicon("awful"))
  }
}
