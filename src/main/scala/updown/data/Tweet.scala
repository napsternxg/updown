package updown.data

class Tweet(val id:String,
            val userid:String,
            val features:List[String],
            val goldLabel:String,
            var systemLabel:String) {

  def this(id:String, userid:String, features:List[String], goldLabel:String) {
    this(id, userid, features, goldLabel, null)
  }
  override def toString = "id: " + id + "\t" + "userid: " + userid + "\t" + "features: " + features + "\t" + "goldLabel: " + goldLabel + "\tsystemLabel: " + systemLabel
}
