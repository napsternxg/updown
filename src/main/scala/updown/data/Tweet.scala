package updown.data

class Tweet(val id: String,
            val userid: String,
            val features: List[String],
            val goldLabel: String,
            var systemLabel: String) {
  /* val POS_ALPHA = "POS"; val POS_INT = "1"
  val NEG_ALPHA = "NEG"; val NEG_INT = "-1"
  val NEU_ALPHA = "NEU"; val NEU_INT = "0"
  */
  def this(id: String, userid: String, features: List[String], goldLabel: String) {
    this (id, userid, features, goldLabel.toString.trim, null)
  }

  override def toString = "id: " + id + "\t" + "userid: " + userid + "\t" + "features: " + features + "\t" + "goldLabel: " + goldLabel + "\tsystemLabel: " + systemLabel

  override def equals(other: Any):Boolean = {
    if (other != null && other.isInstanceOf[Tweet]) {
      val otherTweet = other.asInstanceOf[Tweet]
      (this.id == otherTweet.id) &&
        (this.userid == otherTweet.userid) &&
        (this.features == otherTweet.features) &&
        (this.goldLabel == otherTweet.goldLabel) &&
        (this.systemLabel == otherTweet.systemLabel)
    }
    else {
      false
    }

  }

  /*
   * param must contain string "alpha" or "int" -- former to map into alphas, latter to map into ints.
  */
  def normalize(res: String): Tweet = {
    var sl = ""
    val intToAlpha = List("-1", "0", "1") zip List("NEG", "NEU", "POS")
    if (this.systemLabel != null && res.contains("alpha")) {
      if (this.systemLabel.contains("-1")) sl = "NEG"
      else if (this.systemLabel.contains("0")) sl = "NEU"
      else if (this.systemLabel.contains("1")) sl = "POS"
      else sl = this.systemLabel //if systemLabel is already alpha
    }
    else if (this.systemLabel != null && res.contains("int")) {
      if (this.systemLabel == "NEG") sl = "-1"
      else if (this.systemLabel == "NEU") sl = "0"
      else if (this.systemLabel == "POS") sl = "1"
      else sl = this.systemLabel //if systemLabel is already alpha
    }
    new Tweet(this.id, this.userid, this.features, this.goldLabel.toString.trim, sl.toString.trim)
  }

}
	   
	



