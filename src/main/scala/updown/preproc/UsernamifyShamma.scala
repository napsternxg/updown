package updown.preproc

object UsernamifyShamma {
  def main(args: Array[String]) = {
    val userIdsToTweetIds = new scala.collection.mutable.HashMap[String, String] { override def default(s: String) = "" }
    scala.io.Source.fromFile(args(0)).getLines.foreach(l => if(l.split(" ").length >= 2) userIdsToTweetIds.put(l.split(" ")(1), l.split(" ")(0)))
    //userIdsToTweetIds.toList.foreach(println)

    val userIdsToUserIds = new scala.collection.mutable.HashMap[String, String]
    scala.io.Source.fromFile(args(1)).getLines.foreach(l => if(l.split(" ").length >= 2) userIdsToUserIds.put(l.split(" ")(0), l.split(" ")(1)))
    //userIdsToUserIds.toList.foreach(println)

    val tweetIdsToUsernames = new scala.collection.mutable.HashMap[String, String]
    scala.io.Source.fromFile(args(2)).getLines.foreach(l => tweetIdsToUsernames.put(l.split("\\|")(0), l.split("\\|")(1)))
    //tweetIdsToUsernames.toList.foreach(println)

    for((userid1, userid2) <- userIdsToUserIds) {
      val tweetid1 = userIdsToTweetIds(userid1)
      val tweetid2 = userIdsToTweetIds(userid2)
      
      if(tweetIdsToUsernames.contains(tweetid1) && tweetIdsToUsernames.contains(tweetid2)) {
        val username1 = tweetIdsToUsernames(tweetid1)
        val username2 = tweetIdsToUsernames(tweetid2)
        println(username1+"\t"+username2)
      }
    }
  }
}
