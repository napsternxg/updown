package updown.preproc

import updown.util._
import java.io._
import au.com.bytecode.opencsv.CSVReader

object PreprocHCRTweets {

  val HCR_POS = "positive"
  val HCR_NEG = "negative"
  val HCR_NEU = "neutral"

  // TODO: refactor this into a seperate enum
  val NUM_POS = "1"
  val NUM_NEU = "0"
  val NUM_NEG = "-1"

  def main(args: Array[String]) {
    val reader = new CSVReader(new InputStreamReader(new FileInputStream(new File(args(0))), "UTF-8"))
    val stoplist = scala.io.Source.fromFile(args(1), "utf-8").getLines.toSet
    val targetWriter = if (args.length >= 3) new OutputStreamWriter(new FileOutputStream(new File(args(2))), "UTF-8") else null
    val featureWriter = if (args.length >= 4) new OutputStreamWriter(new FileOutputStream(new File(args(3))), "UTF-8") else null

    var numTweets = 0
    var numPos = 0 //takes on a new meaning with multiple target labels
    var numNeg = 0 //same deal here

    var fields = reader.readNext

    while (fields != null) {
      if (fields.length >= 6) {

        try {

          val tweetid = fields(0).toLong.toString.trim // converting to Long and back ensures id is well formed
          val username = fields(2).trim
          val tweet = fields(3).trim
          val (sentiment1, target1) = (fields(4).trim, fields(5).trim)

          if ((sentiment1 == HCR_POS || sentiment1 == HCR_NEG || sentiment1 == HCR_NEU)
            && tweetid.length > 0 && username.length > 0 && tweet.length > 0 && target1.length > 0) {

            val tokens = BasicTokenizer(tweet)
            val features = tokens.filterNot(stoplist(_)) ::: StringUtil.generateBigrams(tokens)

            var label1 = "";
            var numLabels = 1
            if (sentiment1 == HCR_POS) label1 = NUM_POS
            else if (sentiment1 == HCR_NEG) label1 = NUM_NEG
            else if (sentiment1 == HCR_NEU) label1 = NUM_NEU

            if (label1 == NUM_POS) numPos += 1
            else if (label1 == NUM_NEG) numNeg += 1

            print(tweetid + "|" + username + "|")
            for (feature <- features) print(feature + ",")
            println(label1 + " ") //I think I'm going to axe this trialing space next opportunity i get

            if (targetWriter != null) {
              if (numLabels == 1) targetWriter.write(tweetid + "\t" + target1 + "\n")
            }
            if (featureWriter != null) {
              var text = tweetid + "|" + username + "|"
              for (feature <- features) text += feature + ","

              if (numLabels == 1) text += label1
              text += "\n"
              featureWriter.write(text)
            }
          }
        }
        catch {
          case e: Exception => System.err.println("Error processing tweet at line #" + (numTweets + 1)
            +": "+java.util.Arrays.toString(fields.asInstanceOf[Array[java.lang.Object]]))
        }
      }

      fields = reader.readNext
      numTweets += 1
    }

    reader.close
    if (targetWriter != null) targetWriter.close
    if (featureWriter != null) featureWriter.close

    System.err.println("Preprocessed " + numTweets + " tweets. Fraction positive: " + (numPos.toFloat / numTweets) + "\tFraction Negative: " + (numNeg.toFloat / numTweets)
      + "\tFraction Neutral: " + (1 - ((numPos.toFloat / numTweets) + (numNeg.toFloat / numTweets))).toFloat)
  }
}
