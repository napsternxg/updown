package updown.util

import util.matching.Regex

object TokenizationPipes {
  val twokenize: (List[String]) => List[String] =
    (ss) => ss.map((s) => Twokenize(s)).flatten

  val twokenizeSkipGtOneGrams: (List[String]) => List[String] =
    (ss) => ss.map((s) => if (s.contains(" "))
      List(s)
    else
      Twokenize(s)).flatten

  val toLowercase: (List[String]) => List[String] =
    (ss) => ss.map((s) => s.toLowerCase)

  val splitOnDelimiter: (String) => (List[String]) => List[String] =
    (d) =>
      (ss) => ss.map((s) => s.split(d).toList).flatten

  val filterOnStopset: (Set[String]) => (List[String]) => List[String] =
    (stopSet) =>
      (ss) => ss.filter((s) => !stopSet.contains(s))

  val filterOnRegex: (String)=> (List[String]) => List[String] =
    (regex) =>
      (ss) => ss.filter((s) => s.matches(regex))

  /*
    A really diligent implementation would put (n-1) "$"s at the beginning and end of the
    list, but I kind of doubt that's what we really want, so I'm not going to bother right now.
  */
  val nGrams: (Int) => (List[String]) => List[String] =
    (n) =>
      (ss) => ("$" :: ss ::: ("$" :: Nil)).sliding(n).map(innerList => innerList.mkString(" ")).toList

  val addNGrams: (Int) => (List[String]) => List[String] =
    (n) =>
      (ss) => ss ::: nGrams(n)(ss)
}