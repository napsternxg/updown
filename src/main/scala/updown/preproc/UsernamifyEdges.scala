package updown.preproc

object UsernamifyEdges {
  def main(args: Array[String]) = {
    
    val userIdsToUsernames = new scala.collection.mutable.HashMap[Int, String]

    for(line <- scala.io.Source.fromFile(args(1),"utf-8").getLines) {
      val tokens = line.split("\t")
      if(tokens.length >= 2 && tokens(0).length > 0 && tokens(1).length > 0) {
        userIdsToUsernames.put(tokens(0).toInt, tokens(1))
      }
    }

    for(line <- scala.io.Source.fromFile(args(0),"utf-8").getLines) {
      val tokens = line.split("\t")
      if(tokens.length >= 2 && tokens(0).length > 0 && tokens(1).length > 0) {
        val userId1 = tokens(0).toInt
        val userId2 = tokens(1).toInt
        if(userIdsToUsernames.contains(userId1) && userIdsToUsernames.contains(userId2)) {
          println(userIdsToUsernames(userId1) + "\t" + userIdsToUsernames(userId2))
        }
      }
    }
  }
}
