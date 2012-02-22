package updown.preproc

import updown.util.Commandable

object Run extends Commandable {
  val usageString = "Usage:\n" +
    "preprocess DATASET args...\n" +
    "\tWhere DATASET is one of:\n" +
    "\t- dp\n" +
    "\t- hcr\n" +
    "\t- mdsd\n" +
    "\t- polarity\n" +
    "\t- shamma\n" +
    "\t- stanford\n"

  def apply(args: Array[String]) {
    if (args.length < 1) {
      usage()
    }
    val command = args(0)
    val rest = args.slice(1,args.length)
    command match {
      case "dp" => updown.preproc.impl.PreprocDPArticles(rest)
      case "hcr" => updown.preproc.impl.PreprocHCRTweets(rest)
      case "mdsd" => updown.preproc.impl.PreprocMDSDReviews(rest)
      case "polarity" => updown.preproc.impl.PreprocPangLeePolarityCorpus(rest)
      case "shamma" => updown.preproc.impl.PreprocShammaTweets(rest)
      case "stanford" => updown.preproc.impl.PreprocStanfordTweets(rest)
      case _ =>
        unrecognized(command)
    }
  }
}