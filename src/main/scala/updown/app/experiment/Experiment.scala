package updown.app.experiment

import updown.util.Statistics
import org.clapper.argot.{ArgotParser, SingleValueOption}
import org.clapper.argot.ArgotConverters._
import com.weiglewilczek.slf4s.Logging
import updown.data.{SentimentLabel, TargetedSystemLabeledTweet, SystemLabeledTweet}

abstract class Experiment extends Logging {
  val parser = new ArgotParser(this.getClass.getName, preUsage = Some("Updown"))
  val targetsInputFile = parser.option[String](List("t", "targets"), "targets", "targets")
  val reportFormatO = parser.option[String]("format","tex|txt","default is txt") {
    (s,opt) =>
      s.toLowerCase() match {
        case "tex" => "tex"
        case "txt" => "txt"
        case _     => parser.usage("Format must be 'tex' or 'txt'.")
      }
  }
  val reportNameO = parser.option[String]("name","STRING","The name to use in reports. If unspecified, the name will " +
    "be composed of the input files.")


  def reportTex(experimentName: String, labeledTweets: List[SystemLabeledTweet]) {
    val outputName = reportNameO.value match {
      case Some(s:String) => s
      case _ => experimentName
    }
    val ExperimentalResult(_,eN,accuracy,classes) = Statistics.getEvalStats("",labeledTweets)
    lazy val varName:(String)=>(String)=>(String)=>String = (prefix)=>(suffix)=>(value)=>"\\newcommand{\\%s%s}{%s}".format(prefix,suffix,value)
    lazy val nName = varName("n")(outputName)
    lazy val accName = varName("acc")(outputName)
    lazy val fposName = varName("fpos")(outputName)
    lazy val fnegName = varName("fneg")(outputName)
    println("% "+outputName)
    println(nName("%d".format(eN)))
    println(accName("%.2f".format(accuracy)))
    val classesMap = classes.groupBy(res=>res.label)
    println(fposName("%.2f".format(classesMap(SentimentLabel.Positive)(0).f)))
    println(fnegName("%.2f".format(classesMap(SentimentLabel.Negative)(0).f)))
  }

  def reportTxt(experimentName: String, labeledTweets: List[SystemLabeledTweet]) {
    println("\n-----------------------------------------------------")
    println(experimentName+":")
    println("Overall:\n" + Statistics.getEvalStats("", labeledTweets).toString)

    val (msePerUser, nUsers) = Statistics.getMSEPerUser(labeledTweets)
    println("Per-user Summary:\nN users:%d\n%s\n%s".format(nUsers, "%15s %5s %7s".format("Label", "MSE", "√(MSE)"),
      msePerUser.map {
        case (label, mse) => "%15s %.3f   %.3f".format(SentimentLabel.toEnglishName(label), mse, math.sqrt(mse))
      }.mkString("\n")))

    targetsInputFile.value match {
      case Some(filename) =>
        val targets: Map[String, String] =
          (for (line <- scala.io.Source.fromFile(filename, "UTF-8").getLines) yield {
            val arr = line.trim.split("\\|")
            (arr(0) -> arr(1))
          }).toMap.withDefault(_ => "UNKNOWN")

        val targetedTweets = labeledTweets.map {
          case SystemLabeledTweet(id, uid, features, gLabel, sLabel) =>
            TargetedSystemLabeledTweet(id, uid, features, gLabel, sLabel, targets(id))
        }
        val (statsPerTarget, nTargets) = Statistics.getEvalStatsPerTarget("", targetedTweets)
        if (statsPerTarget.length > 0) {
          println("\nPer-target:\nN targets: %d\n%s".format(nTargets, statsPerTarget.mkString("\n")))
        } else
          println("\nPer-target: No targets were over the threshold")
      case None =>
        println("\nPer-target: No target file provided")
    }
  }
  def report(experimentName: String, labeledTweets: List[SystemLabeledTweet]) {
    reportFormatO.value match {
      case Some("tex") => reportTex(experimentName,labeledTweets)
      case _ => reportTxt(experimentName,labeledTweets)
    }
  }
}