package updown.app.experiment

import updown.util.Statistics
import org.clapper.argot.{ArgotParser, SingleValueOption}
import org.clapper.argot.ArgotConverters._
import com.weiglewilczek.slf4s.Logging
import updown.data.{SentimentLabel, TargetedSystemLabeledTweet, SystemLabeledTweet}

abstract class Experiment extends Logging {
  val parser = new ArgotParser(this.getClass.getName, preUsage = Some("Updown"))
  val targetsInputFile = parser.option[String](List("t", "targets"), "targets", "targets")

  def report(labeledTweets: List[SystemLabeledTweet]) {
    logger.info("Overall:\n" + Statistics.getEvalStats("", labeledTweets).toString)

    val (msePerUser, nUsers) = Statistics.getMSEPerUser(labeledTweets)
    logger.info("Per-user Summary:\nN users:%d\n%s\n%s".format(nUsers, "%15s %5s".format("Label","MSE"),msePerUser.map{case LabelResult(_,label,_,_,_,mse)=>"%15s %.3f".format(SentimentLabel.toEnglishName(label),mse)}.mkString("\n")))

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
        if (statsPerTarget.length > 0){
          logger.info("Per-target:\nN targets: %d\n%s".format(nTargets, statsPerTarget.mkString("\n")))
        }else
          logger.info("Per-target: No targets were over the threshold")
      case None =>
        logger.info("Per-target: No target file provided")
    }
  }
}