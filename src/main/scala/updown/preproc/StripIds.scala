package updown.preproc

object StripIds {

  val lineRE = """^[^|]+\|[^|]+\|(.*)$""".r

  def main(args: Array[String]) = {
    for(line <- scala.io.Source.fromFile(args(0)).getLines) {
      val lineRE(stripped) = line
      println(stripped)
    }
  }
}
