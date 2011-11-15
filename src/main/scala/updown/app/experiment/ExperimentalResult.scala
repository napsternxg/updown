package updown.app.experiment

import updown.data.SentimentLabel

case class ExperimentalResult(name: String, n: Int, accuracy: Double, classes: List[LabelResult]) {
  def header: String = "\n%15s%7s%9s%11s%8s%9s\n".format("Label", "NGold", "NSystem", "Precision", "Recall", "F-Score")

  override def toString(): String =
    "%s Results:\n".format(name) +
      "%10s%6d\n".format("N", n) +
      "%10s%6.2f\n".format("Accuracy", accuracy) +
      header +
      (for (res <- classes) yield res.toString).mkString("\n") + "\n"

  def rename(newName: String): ExperimentalResult =
    ExperimentalResult(newName, n,accuracy,classes)

  def +(other: ExperimentalResult): ExperimentalResult = {
    val classesMap = (classes.groupBy((labelResult) => labelResult.label).map((tup) => {
      val (k, (v: LabelResult) :: vs) = tup
      (k, v)
    })).toMap
    val otherClassesMap = (other.classes.groupBy((labelResult) => labelResult.label).map((tup) => {
      val (k, (v: LabelResult) :: vs) = tup
      (k, v)
    }).toMap).withDefaultValue(LabelResult(0, 0, SentimentLabel.Abstained, 0.0, 0.0, 0.0))
    ExperimentalResult(name, n + other.n, accuracy + other.accuracy,
      (for ((label, classResult) <- classesMap.toList) yield classResult + otherClassesMap(label)).toList
    )
  }

  def *(other: ExperimentalResult): ExperimentalResult = {
    val classesMap = (classes.groupBy((labelResult) => labelResult.label).map((tup) => {
      val (k, (v: LabelResult) :: vs) = tup
      (k, v)
    })).toMap
    val otherClassesMap = (other.classes.groupBy((labelResult) => labelResult.label).map((tup) => {
      val (k, (v: LabelResult) :: vs) = tup
      (k, v)
    }).toMap).withDefaultValue(LabelResult(0, 0, SentimentLabel.Abstained, 0.0, 0.0, 0.0))
    ExperimentalResult(name, n * other.n, accuracy * other.accuracy,
      (for ((label, classResult) <- classesMap.toList) yield classResult * otherClassesMap(label)).toList
    )
  }

  def -(other: ExperimentalResult): ExperimentalResult = {
    val negOther = other * -1
    this + negOther
  }

  def /(scalar: Double): ExperimentalResult = {
    ExperimentalResult(name, (n.toFloat / scalar).toInt, accuracy / scalar,
      (for (labelResult <- classes) yield labelResult / scalar).toList
    )
  }

  def *(scalar: Double): ExperimentalResult = {
    ExperimentalResult(name, (n.toFloat * scalar).toInt, accuracy * scalar,
      (for (labelResult <- classes) yield labelResult * scalar).toList
    )
  }
}


case class LabelResult(nGold: Int, nSystem: Int, label: SentimentLabel.Type, precision: Double, recall: Double, f: Double) {
  override def toString(): String = "%15s%7d%9d%11.2f%8.2f%9.2f".format(SentimentLabel.toEnglishName(label), nGold, nSystem, precision, recall, f)

  def +(other: LabelResult): LabelResult = {
    assert(label == other.label)
    LabelResult(nGold + other.nGold, nSystem + other.nSystem, label, precision + other.precision, recall + other.recall, f + other.f)
  }

  def *(other: LabelResult): LabelResult = {
    assert(label == other.label)
    LabelResult(nGold * other.nGold, nSystem * other.nSystem, label, precision * other.precision, recall * other.recall, f * other.f)
  }

  def /(scalar: Double): LabelResult = LabelResult((nGold.toFloat / scalar).toInt, (nSystem.toFloat / scalar).toInt, label, precision / scalar, recall / scalar, f / scalar)

  def *(scalar: Double): LabelResult = LabelResult((nGold.toFloat * scalar).toInt, (nSystem.toFloat * scalar).toInt, label, precision * scalar, recall * scalar, f * scalar)
}
