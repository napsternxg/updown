package updown.test

import org.scalatest.FlatSpec
import updown.util.{NaiveBayesModel, BasicTokenizer}
import updown.data.{SystemLabeledTweet, SentimentLabel, GoldLabeledTweet}

class NBayesModelTest extends FlatSpec {
  val t0 = GoldLabeledTweet("0","",List("foo","bar","baz"),SentimentLabel.Positive)
  val t1 = GoldLabeledTweet("1","",List("foo","bar","baz"),SentimentLabel.Positive)
  val t2 = GoldLabeledTweet("2","",List("bar","baz","foo"),SentimentLabel.Positive)
  val t3 = GoldLabeledTweet("3","",List("bar","baz","foo"),SentimentLabel.Positive)
  val t4 = GoldLabeledTweet("4","",List("foo","bar","foo","baz"),SentimentLabel.Positive)

  val t5 = GoldLabeledTweet("5","",List("cat","dog","bird"),SentimentLabel.Negative)
  val t6 = GoldLabeledTweet("6","",List("foo","dog","bird"),SentimentLabel.Negative)
  val t7 = GoldLabeledTweet("7","",List("dog","bird","cat"),SentimentLabel.Negative)
  val t8 = GoldLabeledTweet("8","",List("dog","bird","cat"),SentimentLabel.Negative)
  val t9 = GoldLabeledTweet("9","",List("cat","dog","cat","bird"),SentimentLabel.Negative)

  val autoclassify:(GoldLabeledTweet) => SystemLabeledTweet =
  g=>SystemLabeledTweet(g.id,g.userid,g.features,g.goldLabel,g.goldLabel)

  "classify" should "work" in {
    val   train = List(t0,t1,t2,t3,t5,t6,t7,t8)
    val model = new NaiveBayesModel(train)
    assert(model.classify(t4) === autoclassify(t4))
    assert(model.classify(t5) === autoclassify(t5))
  }
}