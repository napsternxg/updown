package updown.app.experiment.topic.util

import updown.data.SentimentLabel
import opennlp.maxent.GIS
import opennlp.model.DataIndexer

trait KNNDiscriminant {

  private def euclideanDist(a: Array[Float], b: Array[Float]): Float = {
    var result: Double = 0.0
    for (i <- 0 until a.length) {
      result += math.pow(a(i) - b(i), 2)
    }
    math.sqrt(result).toFloat
  }

  private def getNearestNeighbors(k: Int)(self: Array[Float], neighborhood: List[(SentimentLabel.Type, Array[Float])]): List[(SentimentLabel.Type, Float)] = {
    neighborhood.map {
      case (label, position) => (label, euclideanDist(self, position))
    }.sortBy {
      case (label, dist) => dist
    }.take(k)
  }

  def getDiscriminantFn(k: Int, labelsToTopicDists: Map[SentimentLabel.Type, scala.List[Array[Double]]]): (Array[Float]) => (String, String) = {
    val posLabelList: List[(SentimentLabel.Type, Array[Double])] =
      (for ((label, posList) <- labelsToTopicDists.toList) yield (for (pos <- posList) yield (label, pos)).toList).toList.flatten
    val posLabelListFloat: List[(SentimentLabel.Type, Array[Float])] =
      posLabelList.map {
        case (label, pos) => (label, pos.map(d => d.asInstanceOf[Float]))
      }
    val getKNearestNeighbors = getNearestNeighbors(k)_

    (topicDist: Array[Float]) => {
      val nearestNeighbors = getKNearestNeighbors(topicDist,posLabelListFloat)
      val sizes = nearestNeighbors.groupBy{case(label,dist)=>label}.map{case(label,list)=>(label,list.size)}.toList.sortBy{case(label,size)=>size}
      val (winningLabel,dist) = sizes.last
      (winningLabel.toString, sizes.toString)
    }
  }
}