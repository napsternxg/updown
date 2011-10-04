package updown.app.experiment.topic

import updown.data.{SystemLabeledTweet, GoldLabeledTweet, SentimentLabel}
import updown.util.{Statistics, LDATopicModel, TopicModel}
import updown.app.experiment.NFoldExperiment

abstract class NFoldTopicExperiment extends NFoldExperiment {

  def evaluate(model: TopicModel, testSet: scala.List[GoldLabeledTweet]):
  (Double, scala.List[(SentimentLabel.Type, Double, Double, Double)])

  def doExperiment(testSet: List[GoldLabeledTweet], trainSet: List[GoldLabeledTweet]) = {
    val model: TopicModel = new LDATopicModel(trainSet, 3, 1000, 100, 0.1)

    logger.info("topic distribution:\n     :" + model.getTopicPriors)
    logger.info({
      val labelToTopicDist = model.getTopicsPerTarget
      "topic distribution over labels:\n" + (for ((k, v) <- labelToTopicDist) yield "%5s:%s".format(k, v)).mkString("\n")
    })
    logger.info({
      val topics = model.getTopics
      "topic distributions\n" +
        (for (i <- 0 until 3) yield "%5s: Topic(%s,%s)".format(i, topics(i).prior, topics(i).distribution.toList.sortBy((pair) => (1 - pair._2)))).mkString("\n")
    })
    evaluate(model, testSet)
  }
}