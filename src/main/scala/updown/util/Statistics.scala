package updown.util

import com.weiglewilczek.slf4s.Logging
import updown.app.experiment.{LabelResult, ExperimentalResult}
import java.io.{OutputStreamWriter, BufferedOutputStream}
import updown.data.{Tweet, TargetedSystemLabeledTweet, SentimentLabel, SystemLabeledTweet}

object Statistics extends Logging {

  val MinTPU: Int = 3
  val MinTPT: Int = 3

  def mean(list: List[ExperimentalResult]): ExperimentalResult = {
    (list.reduce(_ + _) / list.length).rename("Mean")
  }

  def variance(list: List[ExperimentalResult]): ExperimentalResult = {
    val list_mean = mean(list)
    mean(list.map((obj) => (obj - list_mean) * (obj - list_mean))).rename("Variance")
  }

  val accurracy: (Double, Double) => Double =
    (correct, total) => correct / total
  val precision: (Double, Double) => Double =
    (numCorrectlyLabeled, totalNumLabeled) => numCorrectlyLabeled / totalNumLabeled
  val recall: (Double, Double) => Double =
    (numCorrectlyLabeled, numberThatShouldHaveBeenLabeled) => numCorrectlyLabeled / numberThatShouldHaveBeenLabeled
  val fScore: (Double, Double) => Double =
    (precision, recall) => 2.0 * precision * recall / (precision + recall)

  val dot: (List[Double], List[Double]) => Double =
    (A, B) => {
      assert(A.length == B.length)
      (0.0 /: (A zip B).map {
        case (a, b) => a * b
      }) {
        _ + _
      }
    }

  val mag: (List[Double]) => Double =
    (A) => math.sqrt(A.map((i) => i * i).reduce(_ + _))

  val cosineSimilarity: (List[Double], List[Double]) => Double =
    (A, B) => (dot(A, B) / (mag(A) * mag(B)))

  def tabulate(tweets: scala.List[SystemLabeledTweet]): (Double, Int) = {
    var correct = 0.0
    var total = 0
    var numAbstained = tweets.count(_.systemLabel == null)
    logger.debug("null sys labels: %d".format(tweets.count(_.systemLabel == null)))
    for (tweet <- tweets) {

      if (tweet.systemLabel == tweet.goldLabel) {
        correct += 1
      }

      total += 1
    }
    correct += numAbstained.toFloat / 3

    (correct, total)
  }


  def averageResults(newName: String, results: scala.List[ExperimentalResult]): ExperimentalResult = {
    var avgAccuracy = 0.0
    var avgN = 0.0
    var avgLabelResults = scala.collection.mutable.Map[SentimentLabel.Type, LabelResult]().withDefault((label) => LabelResult(0, label, 0.0, 0.0, 0.0))
    // first, sum
    for (ExperimentalResult(name, n, accuracy, classes) <- results) {
      avgAccuracy += accuracy
      avgN += n
      for (LabelResult(n, label, precision, recall, f) <- classes) {
        val LabelResult(oN, oLabel, oPrecision, oRecall, oF) = avgLabelResults(label)
        avgLabelResults(label) = LabelResult(n + oN, label, precision + oPrecision, recall + oRecall, f + oF)
      }
    }
    // then, scale
    val N = results.length
    ExperimentalResult(newName, (avgN / N).toInt, avgAccuracy / N,
      (for ((_, LabelResult(n, label, precision, recall, f)) <- avgLabelResults.toList.sortBy {
        case (k, v) => SentimentLabel.ordinality(k)
      }) yield {
        LabelResult(n / N, label, precision / N, recall / N, f / N)
      }).toList)
  }

  def getEvalStats(resultName: String, tweets: scala.List[SystemLabeledTweet]): ExperimentalResult = {
    val (correct, total) = tabulate(tweets)
    ExperimentalResult(resultName, total, accurracy(correct, total),
      (for (label <- List(SentimentLabel.Positive, SentimentLabel.Negative, SentimentLabel.Neutral)) yield {
        val goldList = tweets.filter((tweet) => tweet.goldLabel == label)
        logger.debug("%s gold tweets: %d".format(SentimentLabel.toEnglishName(label), goldList.length))
        val systemList = tweets.filter((tweet) => tweet.systemLabel == label)
        logger.debug("%s system tweets: %d".format(SentimentLabel.toEnglishName(label), systemList.length))
        val labelPrecision = precision(
          systemList.filter((tweet) => tweet.goldLabel == label).length,
          systemList.length)
        val labelRecall = recall(
          goldList.filter((tweet) => tweet.systemLabel == label).length,
          goldList.length
        )

        LabelResult(goldList.length, label, labelPrecision, labelRecall, fScore(labelPrecision, labelRecall))
      }).toList)
  }

  def getMSEPerUser(tweets: scala.List[SystemLabeledTweet]): (List[(SentimentLabel.Type,Double)], Int) = {
    var totalError = 0.0;
    var totalErrorPos = 0.0
    var totalErrorNeg = 0.0
    var totalErrorNeu = 0.0
    var totalNumAbstained = 0
    val usersToTweets = new scala.collection.mutable.HashMap[String, List[Tweet]] {
      override def default(s: String) = List()
    }

    for (tweet <- tweets) usersToTweets.put(tweet.userid, usersToTweets(tweet.userid) ::: (tweet :: Nil))

    val usersToTweetsFiltered = usersToTweets.filter(p => p._2.length >= MinTPU)
    var users = 0
    var utweets = 0
    for (userid <- usersToTweetsFiltered.keys) {
      users += 1
      val curTweets = usersToTweetsFiltered(userid)

      var numAbstained = 0
      if (curTweets.length >= MinTPU) {
        var numGoldPos = 0.0;
        var numSysPos = 0.0
        var numGoldNeg = 0.0;
        var numSysNeg = 0.0
        var numGoldNeu = 0.0;
        var numSysNeu = 0.0

        for (tweet <- curTweets) {
          utweets += 1
          tweet match {
            case SystemLabeledTweet(_, _, _, SentimentLabel.Positive, _) => numGoldPos += 1
            case SystemLabeledTweet(_, _, _, SentimentLabel.Negative, _) => numGoldNeg += 1
            case SystemLabeledTweet(_, _, _, SentimentLabel.Neutral, _) => numGoldNeu += 1
          }
          {
            tweet match {
              case SystemLabeledTweet(_, _, _, _, SentimentLabel.Positive) => numSysPos += 1
              case SystemLabeledTweet(_, _, _, _, SentimentLabel.Negative) => numSysNeg += 1
              case SystemLabeledTweet(_, _, _, _, SentimentLabel.Neutral) => numSysNeu += 1
              case _ => numAbstained += 1
            }
          }
        }

        totalError += math.pow(((numGoldPos + numGoldNeg + numGoldNeu) - (numSysPos + numSysNeg + numSysNeu)) / curTweets.length, 2)
        totalErrorPos += math.pow(((numGoldPos) - (numSysPos)) / curTweets.length, 2)
        totalErrorNeg += math.pow(((numGoldNeg) - (numSysNeg)) / curTweets.length, 2)
        totalErrorNeu += math.pow(((numGoldNeu) - (numSysNeu)) / curTweets.length, 2)
        totalNumAbstained += numAbstained
      }
    }

    totalError /= usersToTweetsFiltered.size
    totalErrorPos /= usersToTweetsFiltered.size
    totalErrorNeg /= usersToTweetsFiltered.size
    totalErrorNeu /= usersToTweetsFiltered.size

    (List((SentimentLabel.Positive, totalErrorPos),
      (SentimentLabel.Negative, totalErrorNeg),
      (SentimentLabel.Neutral, totalErrorNeu)), usersToTweetsFiltered.size)
  }

  def getEvalStatsPerTarget(resultName: String, tweets: scala.List[TargetedSystemLabeledTweet]): (List[ExperimentalResult], Int) = {
    val targetToTweets = tweets.groupBy((tweet) => tweet.target).toList.filter {
      case (target, tweets) =>
        tweets.length > MinTPT
    }.sortBy {
      case (target, tweets) => tweets.length
    }.reverse
    ((for ((target, tweets) <- targetToTweets) yield {
      val res = Statistics.getEvalStats("%s %s".format(resultName, target), tweets.map {
        case TargetedSystemLabeledTweet(id, uid, features, gLabel, sLabel, target) => SystemLabeledTweet(id, uid, features, gLabel, sLabel)
      })
      res
    }).toList, targetToTweets.length)
  }
}