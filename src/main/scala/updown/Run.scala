package updown

import util.Commandable

object Run extends Commandable{
  val usageString = "Usage:\n" +
    "Run COMMAND args...\n" +
    "\tWhere COMMAND is one of:\n" +
    "\t- preprocess\n" +
    "\t- experiment"

  def main(args: Array[String]) {
    if (args.length < 1) {
      usage()
    }
    val command = args(0)
    val rest = args.slice(1,args.length)
    command match {
      case "preprocess" => updown.preproc.Run(rest.toArray)
      case "experiment" => updown.app.experiment.Run(rest.toArray)
      case _ => unrecognized(command)
    }
  }
}