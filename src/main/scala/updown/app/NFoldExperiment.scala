package updown.app

import updown.data.io.TweetFeatureReader
import updown.data.{SentimentLabel, GoldLabeledTweet}

abstract class NFoldExperiment {
  def generateTrials(inputFile: String, nFolds: Int): Iterator[(List[GoldLabeledTweet], List[GoldLabeledTweet])] = {
    val foldsToTweets = (for ((fold, list) <- TweetFeatureReader(inputFile).zipWithIndex.groupBy((pair) => {
      val (_, index) = pair;
      index % nFolds
    })) yield {
      (fold, list.map((pair) => {
        val (tweet, _) = pair;
        tweet
      }))
    }).toList

    (for ((heldOutFold, heldOutData) <- foldsToTweets) yield {
      (heldOutData,
        foldsToTweets.filter((pair) => {
          val (listFold, _) = pair;
          listFold != heldOutFold
        }).map((pair) => {
          val (_, tweets) = pair;
          tweets
        }).flatten)
    }).iterator
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