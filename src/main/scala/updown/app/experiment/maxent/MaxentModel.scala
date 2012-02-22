package updown.app.experiment.maxent

import org.clapper.argot.ArgotParser
import org.clapper.argot.ArgotConverters.{convertDouble,convertInt}

trait MaxentModel {
  val parser: ArgotParser
  val sigmaOption = parser.option[Double]("sigma","DOUBLE",
    "the value for gaussian smoothing in training the maxent model")
  val iterationsOption = parser.option[Int]("iterations","INT",
    "the number of iterations in training the maxent model")
}