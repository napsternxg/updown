package updown.data

abstract class Tweet()

case class GoldLabeledTweet(id: String,
                            userid: String,
                            features: List[String],
                            goldLabel: SentimentLabel.Type) extends Tweet

case class TargetedGoldLabeledTweet(id: String,
                                    userid: String,
                                    features: List[String],
                                    goldLabel: SentimentLabel.Type,
                                    target: String) extends Tweet


case class SystemLabeledTweet(id: String,
                              userid: String,
                              features: List[String],
                              goldLabel: SentimentLabel.Type,
                              systemLabel: SentimentLabel.Type) extends Tweet


case class TargetedSystemLabeledTweet(id: String,
                                      userid: String,
                                      features: List[String],
                                      goldLabel: SentimentLabel.Type,
                                      systemLabel: SentimentLabel.Type,
                                      target: String) extends Tweet




