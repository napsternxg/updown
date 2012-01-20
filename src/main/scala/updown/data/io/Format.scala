package updown.data.io

import java.io.File
import updown.data.{GoldLabeledTweet, Tweet}

abstract class Format {
  def read(inputFile:File): Iterator[GoldLabeledTweet]
  def write(outputFile:File, instances:Iterator[GoldLabeledTweet])
}