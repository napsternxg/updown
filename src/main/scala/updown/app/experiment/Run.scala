package updown.app.experiment

import updown.util.Commandable

object Run extends Commandable{
  val usageString = "Usage:\n" +
    "experiment TYPE EXPERIMENT args...\n" +
    "\tWhere TYPE is one of:\n" +
    "\t- static\n" +
    "\t- split\n" +
    "\t- nfold\n\n" +
    "\tWhere EXPERIMENT is one of:\n" +
    "\t- junto\n" +
    "\t- lexical\n" +
    "\t- maxent\n" +
    "\t- nbayes\n" +
    "\t- lda-maxent\n\n" +
    "\t- plda-maxent\n\n" +
    "\t Note that some combinations may not be implemented."

  def apply(args: Array[String]) {
    if (args.length < 2) {
      usage()
    }
    val exptype = args(0)
    val command = args(1)
    val rest = args.slice(2,args.length)
    command match {
      case "junto" =>
        exptype match {
          case "split" => updown.app.experiment.labelprop.SplitJuntoExperiment(rest)
          case "static" => updown.app.experiment.labelprop.StaticJuntoExperiment(rest)
          case "nfold" => notImplemented(command+" "+exptype)
          case _ => unrecognized(command)
        }
      case "lexical" =>
        exptype match {
          case "split" => notImplemented(command+" "+exptype)
          case "static" => updown.app.experiment.lexical.LexicalRatioExperiment(rest)
          case "nfold" => notImplemented(command+" "+exptype)
          case _ => unrecognized(command)
        }
      case "maxent" =>
        exptype match {
          case "split" => updown.app.experiment.maxent.SplitMaxentExperiment(rest)
          case "static" => updown.app.experiment.maxent.StaticMaxentExperiment(rest)
          case "nfold" => updown.app.experiment.maxent.NFoldMaxentExperiment(rest)
          case _ => unrecognized(command)
        }
      case "nbayes" =>
        exptype match {
          case "split" => notImplemented(command+" "+exptype)
          case "static" => notImplemented(command+" "+exptype)
          case "nfold" => updown.app.experiment.nbayes.NFoldNBayesExperiment(rest)
          case _ => unrecognized(command)
        }
      case "lda-maxent" =>
        exptype match {
          case "split" => updown.app.experiment.topic.lda.SplitLDAMaxentExperiment(rest)
          case "static" => notImplemented(command+" "+exptype)
          case "nfold" => updown.app.experiment.topic.lda.NFoldDiscriminantLDAExperiment(rest)
          case _ => unrecognized(command)
        }
      case "plda-maxent" =>
        exptype match {
          case "split" => updown.app.experiment.topic.plda.SplitPLADMaxentExperiment(rest)
          case "static" => notImplemented(command+" "+exptype)
          case "nfold" => notImplemented(command+" "+exptype)
          case _ => unrecognized(command)
        }
      case _ =>
        unrecognized(command)
    }
  }
}