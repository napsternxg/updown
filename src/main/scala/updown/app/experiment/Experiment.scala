package updown.app.experiment

import updown.util.Statistics
import updown.data.{TargetedSystemLabeledTweet, SystemLabeledTweet}
import org.clapper.argot.{ArgotParser, SingleValueOption}
import org.clapper.argot.ArgotConverters._
import com.weiglewilczek.slf4s.Logging

abstract class Experiment extends Logging {
  val parser = new ArgotParser(this.getClass.getName, preUsage = Some("Updown"))
  val targetsInputFile = parser.option[String](List("t", "targets"), "targets", "targets")

  def report(labeledTweets: List[SystemLabeledTweet]) {
    logger.info("Overall:\n" + Statistics.getEvalStats("", labeledTweets).toString)
    val statsPerUser: List[ExperimentalResult] = Statistics.getEvalStatsPerUser("", labeledTweets)
    logger.info("Per-user Summary:\n"+Statistics.mean(statsPerUser)+"\n"+Statistics.variance(statsPerUser))
    if (statsPerUser.length > 0)
      logger.debug("Per-user:\n" + statsPerUser.mkString("\n"))
    else
      logger.info("Per-user: No users were over the threshold.")

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
        val statsPerTarget: List[ExperimentalResult] = Statistics.getEvalStatsPerTarget("", targetedTweets)
        if (statsPerTarget.length > 0){
          logger.info("Per-target Summary:\n"+Statistics.mean(statsPerTarget)+"\n"+Statistics.variance(statsPerTarget))

          logger.debug("Per-target:\n" + statsPerTarget.mkString("\n"))
        }else
          logger.info("Per-target: No targets were over the threshold")
      case None =>
        logger.info("Per-target: No target file provided")
    }
  }
}