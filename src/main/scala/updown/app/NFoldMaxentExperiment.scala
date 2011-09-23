package updown.app

import org.clapper.argot.{ArgotUsageException, ArgotParser}
import org.clapper.argot.ArgotConverters._
import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}

object NFoldMaxentExperiment extends NFoldExperiment {
  // this exists purely to make the ArgotConverters appear used to IDEA
  convertByte _

  def doExperiment(inputFile: String, nFolds: Int) = {
    (for ((testSet, trainSet) <- generateTrials(inputFile, nFolds)) yield {
      val model = TrainMaxentModel.trainWithGoldLabeledTweetIterator(trainSet.iterator)

      PerTweetEvaluator.getEvalStats(for (tweet <- testSet) yield {
        tweet match {
          case GoldLabeledTweet(id, userid, features, goldLabel) =>
            SystemLabeledTweet(id, userid, features, goldLabel,
              SentimentLabel.figureItOut(model.getBestOutcome(model.eval(features.toArray))))
        }
      })
    }).toList
  }

  def initializeAverageList(list: List[(updown.data.SentimentLabel.Type, Double, Double, Double)]): List[(updown.data.SentimentLabel.Type, Double, Double, Double)] = {
    if (list.length == 0)
      Nil
    else {
      val ((lLabel, _, _, _) :: ls) = list
      (lLabel, 0.0, 0.0, 0.0) :: initializeAverageList(ls)
    }
  }

  def addAll(list: List[(updown.data.SentimentLabel.Type, Double, Double, Double)], to: List[(updown.data.SentimentLabel.Type, Double, Double, Double)]): List[(updown.data.SentimentLabel.Type, Double, Double, Double)] = {
    if (list.length == 0)
      Nil
    else {
      val ((lLabel, lPrecision, lRecall, lFScore) :: ls) = list
      val ((tLabel, tPrecision, tRecall, tFScore) :: ts) = to
      assert(lLabel == tLabel)
      (lLabel, lPrecision + tPrecision, lRecall + tRecall, lFScore + tFScore) :: addAll(ls, ts)
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
    var avgLabelResultsList = initializeAverageList(results(0)._2)
    for ((accuracy, labelResults) <- results) {
      avgAccuracy += accuracy
      avgLabelResultsList = addAll(labelResults, avgLabelResultsList)
    }
    avgAccuracy /= results.length
    avgLabelResultsList = divideBy(avgLabelResultsList, results.length)
    //    println(results.mkString("\n"))
    //    println("Averages:")
    //    println("(Accuracy, List((Label, Precision, Recall, F-Score)")
    //    println((avgAccuracy, avgLabelResultsList))
    (avgAccuracy, avgLabelResultsList)

  }

  def main(args: Array[String]) {
    val parser = new ArgotParser("updown run updown.app.PerTweetEvaluator", preUsage = Some("Updown"))
    val goldInputFile = parser.option[String](List("g", "gold"), "gold", "gold labeled input")
    val n = parser.option[Int](List("n", "folds"), "FOLDS", "the number of folds for the experiment (default 10)")

    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(0)
    }

    val nFolds: Int = n.value.getOrElse(10)

    if (goldInputFile.value == None) {
      println("You must specify a gold labeled input file via -g.")
      sys.exit(1)
    }

    val inputFile = goldInputFile.value.get
    val results = doExperiment(inputFile, nFolds)
    val averages = averageResults(results)
    println("\n"+reportResults(averages))
  }
}