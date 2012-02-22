package updown.util

trait Commandable {
  val usageString:String

  def usage() {
    System.err.println(usageString)
    sys.exit(1)
  }

  def unrecognized(command: String) {
    System.err.println("unrecognized command: %s".format(command))
    usage()
  }

  def notImplemented(command: String) {
    System.err.println("%s is not implemented".format(command))
    usage()
  }
}