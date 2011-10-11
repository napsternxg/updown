package updown.util

import updown.data.{SentimentLabel, SystemLabeledTweet}
import com.weiglewilczek.slf4s.Logging

object Statistics extends Logging {

  val accurracy: (Double, Double) => Double =
    (correct, total) => correct / total
  val precision: (Double, Double) => Double =
    (numCorrectlyLabeled, totalNumLabeled) => numCorrectlyLabeled / totalNumLabeled
  val recall: (Double, Double) => Double =
    (numCorrectlyLabeled, numberThatShouldHaveBeenLabeled) => numCorrectlyLabeled / numberThatShouldHaveBeenLabeled
  val fScore: (Double, Double) => Double =
    (precision, recall) => 2.0 * precision * recall / (precision + recall)

  val dot: (List[Double], List[Double]) => Double =
    (A,B) => {
      assert (A.length == B.length)
      (0.0 /: (A zip B).map{case(a,b) => a*b}) {_ + _}
    }

  val mag: (List[Double])=>Double =
    (A) => math.sqrt(A.map((i)=>i*i).reduce(_ + _))

  val cosineSimilarity: (List[Double], List[Double]) => Double =
    (A, B) => (dot(A, B) / (mag(A) * mag(B)))

  def tabulate(tweets: scala.List[SystemLabeledTweet]): (Double, Int) = {
    var correct = 0.0
    var total = 0
    var numAbstained = tweets.count(_.systemLabel == null)

    for (tweet <- tweets) {
      //      println(tweet.systemLabel + "|" + tweet.goldLabel)
      /*
       * val normedTweet = tweet.normalize("alpha")
      *  val normedNormedTweet = normedTweet.normalize("int")
      *  println(normedTweet.systemLabel + "|" + normedTweet.goldLabel + "\t" + normedNormedTweet.systemLabel + "|" + normedNormedTweet.goldLabel)
      */
      //      val normedTweet = tweet.normalize("alpha")
      if (tweet.systemLabel == tweet.goldLabel) {
        correct += 1
      }

      total += 1
    }
    correct += numAbstained.toFloat / 3

    (correct, total)
  }


  def initializeAverageList(list: List[(updown.data.SentimentLabel.Type, Double, Double, Double)]): List[(updown.data.SentimentLabel.Type, Double, Double, Double)] = {
    if (list.length == 0)
      Nil
    else {
      val ((lLabel, _, _, _) :: ls) = list
      (lLabel, 0.0, 0.0, 0.0) :: initializeAverageList(ls)
    }
  }

  def addWithoutNaN(d1: Double, d2: Double): Double = {
    /*if (d1.equals(Double.NaN)) {
      d2
    } else if (d2.equals(Double.NaN)) {
      d1
    } else {
      d1 + d2
    }*/
    d1 + d2
  }

  def addAll(list: List[(updown.data.SentimentLabel.Type, Double, Double, Double)], to: List[(updown.data.SentimentLabel.Type, Double, Double, Double)]): List[(updown.data.SentimentLabel.Type, Double, Double, Double)] = {
    if (list.length == 0)
      Nil
    else {
      val ((lLabel, lPrecision, lRecall, lFScore) :: ls) = list
      val ((tLabel, tPrecision, tRecall, tFScore) :: ts) = to
      assert(lLabel == tLabel)
      (lLabel, addWithoutNaN(lPrecision, tPrecision), addWithoutNaN(lRecall, tRecall), addWithoutNaN(lFScore, tFScore)) :: addAll(ls, ts)
    }
  }

  def divideBy(list: List[(updown.data.SentimentLabel.Type, Double, Double, Double)], by: Double): List[(updown.data.SentimentLabel.Type, Double, Double, Double)] = {
    if (list.length == 0)
      Nil
    else {
      val ((lLabel, lPrecision, lRecall, lFScore) :: ls) = list
      (lLabel, lPrecision / by, lRecall / by, lFScore / by) :: divideBy(ls, by)
    }
  }


  def averageResults(results: scala.List[(Double, scala.List[(SentimentLabel.Type, Double, Double, Double)])]): (Double, scala.List[(SentimentLabel.Type, Double, Double, Double)]) = {
    var avgAccuracy = 0.0
    var avgLabelResultsList = initializeAverageList(results(0)._2).sortBy({case(x,_,_,_)=>SentimentLabel.ordinality(x)})
    for ((accuracy, labelResults) <- results) {
      avgAccuracy += accuracy
      avgLabelResultsList = addAll(labelResults.sortBy({case(x,_,_,_)=>SentimentLabel.ordinality(x)}), avgLabelResultsList)
    }
    avgAccuracy /= results.length
    avgLabelResultsList = divideBy(avgLabelResultsList, results.length)
    (avgAccuracy, avgLabelResultsList)
  }

  def getEvalStats(tweets: scala.List[SystemLabeledTweet]): (Double, List[(SentimentLabel.Type, Double, Double, Double)]) = {
    val (correct, total) = tabulate(tweets)
    (accurracy(correct, total.toDouble),
      (for (label <- List(SentimentLabel.Negative, SentimentLabel.Neutral, )) yield {
        val goldList = tweets.filter((tweet) => tweet.goldLabel == label)
        val systemList = tweets.filter((tweet) => tweet.systemLabel == label)
        val labelPrecision = precision(
          systemList.filter((tweet) => tweet.goldLabel == label).length,
          systemList.length)
        val labelRecall = recall(
          goldList.filter((tweet) => tweet.systemLabel == label).length,
          goldList.length
        )
        (label, labelPrecision, labelRecall, fScore(labelPrecision, labelRecall))
      }).toList)
  }

  def reportResults(resultTuple: (Double, scala.List[(SentimentLabel.Type, Double, Double, Double)])): String = {
    val (accuracy, labelResultsList) = resultTuple
    "Results:\n" +
      "%12s%6.2f\n".format("Accuracy", accuracy) +
      "%12s%11s%8s%9s\n".format("Label", "Precision", "Recall", "F-Score") +
      (for ((label, precision, recall, fScore) <- labelResultsList) yield {
        "%12s%11.2f%8.2f%9.2f".format(SentimentLabel.toEnglishName(label), precision, recall, fScore)
      }).mkString("\n") + "\n"
  }
}