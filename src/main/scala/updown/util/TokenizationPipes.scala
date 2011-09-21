package updown.util

object TokenizationPipes {
  val toLowercase: (List[String]) => List[String] =
    (ss) => ss.map((s) => s.toLowerCase)

  val splitOnDelimiter: (String) => (List[String]) => List[String] =
    (d) =>
      (ss) => ss.map((s) => s.split(d).toList).flatten

  val filterOnStopset: (Set[String]) => (List[String]) => List[String] =
    (stopSet) =>
      (ss) => ss.filter((s) => !stopSet.contains(s))

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