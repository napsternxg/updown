package updown.util

import updown.data.{SentimentLabel, SystemLabeledTweet}

object Statistics {

  val accurracy: (Double, Double) => Double =
    (correct, total) => correct / total
  val precision: (Double, Double) => Double =
    (numCorrectlyLabeled, totalNumLabeled) => numCorrectlyLabeled / totalNumLabeled
  val recall: (Double, Double) => Double =
    (numCorrectlyLabeled, numberThatShouldHaveBeenLabeled) => numCorrectlyLabeled / numberThatShouldHaveBeenLabeled
  val fScore: (Double, Double) => Double =
    (precision, recall) => 2.0 * precision * recall / (precision + recall)

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

  def getEvalStats(tweets: scala.List[SystemLabeledTweet]): (Double, List[(SentimentLabel.Type, Double, Double, Double)]) = {
    val (correct, total) = tabulate(tweets)

    (accurracy(correct, total.toDouble),
      (for (label <- SentimentLabel.values) yield {
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