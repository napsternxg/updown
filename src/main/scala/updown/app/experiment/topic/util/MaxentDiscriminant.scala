package updown.app.experiment.topic.util

import updown.data.SentimentLabel
import opennlp.maxent.GIS
import opennlp.model.DataIndexer

trait MaxentDiscriminant {

  def getDiscriminantFn(labelsToTopicDists: Map[SentimentLabel.Type, scala.List[Array[Double]]]): (Array[Float]) => (String, String) = {
    val discriminantModel = GIS.trainModel(1000, new DataIndexer {
      private var _total = 0
      for ((_, list) <- labelsToTopicDists) {
        _total += list.length
      }
      private val _dimensions = labelsToTopicDists.toList(0)._2(0).size //yikes!

      private val _labels: Array[String] = labelsToTopicDists.keys.toList.map(l => l.toString).toArray
      private val _contexts: Array[Array[Int]] = {
        val result = Array.ofDim[Int](_total, _dimensions)
        for (i <- 0 until _total) {
          for (j <- 0 until _dimensions) {
            result(i)(j) = j
          }
        }
        result
      }
      private val _predLabels: Array[String] = {
        val result = Array.ofDim[String](_dimensions)
        for (j <- 0 until _dimensions) {
          result(j) = j.toString
        }
        result
      }
      private val _predCounts: Array[Int] = Array.fill[Int](_dimensions)(1)

      private val (_eventCounts: Array[Int], _eventOutcomes: Array[Int], _eventValues: Array[Array[Float]]) = {
        var eventCounts = List[Int]()
        var eventOutcomes = List[Int]()
        var eventValues = List[Array[Float]]()
        for ((label, labelIndex) <- _labels.zipWithIndex) {
          val events = labelsToTopicDists(SentimentLabel.figureItOut(label))
          for (event <- events) {
            eventCounts = 1 :: eventCounts
            eventOutcomes = labelIndex :: eventOutcomes
            eventValues = event.map(d => d.asInstanceOf[Float]) :: eventValues
          }
        }
        val (tmp1,tmp2,tmp3)=(eventCounts.toArray, eventOutcomes.toArray, eventValues.toArray)
        (eventCounts.toArray, eventOutcomes.toArray, eventValues.toArray)
      }

      def getContexts = _contexts

      def getPredLabels = _predLabels

      def getPredCounts = _predCounts

      def getNumTimesEventsSeen = _eventCounts

      def getOutcomeList = _eventOutcomes

      def getOutcomeLabels = _labels

      def getValues = _eventValues

      def getNumEvents = _eventCounts.size
    })

    (topicDist: Array[Float]) => {
      val weights = discriminantModel.eval({
        val result = Array.ofDim[String](topicDist.size)
        for (j <- 0 until topicDist.size) {
          result(j) = j.toString
        }
        result
      }, topicDist)
      (discriminantModel.getBestOutcome(weights), discriminantModel.getAllOutcomes(weights))
    }
  }
}