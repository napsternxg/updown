package updown.data

class ProbabilityLexicon {

  var vocabSize = 0
  val wordsToInts = new scala.collection.mutable.HashMap[String, Int] { override def default(s: String) = -1 }

  val unigramCounts = new scala.collection.mutable.HashMap[Int, Int] { override def default(n: Int) = 0 }
  var totalUnigramCount = 0

  val bigramCounts = new scala.collection.mutable.HashMap[(Int, Int), Int] { override def default(t: (Int, Int)) = 0 }

  def observeNgram(ngram: String): Unit = {
    if(ngram.contains(" ")) {
      val tokens = ngram.split(" ")
      observeBigram(tokens(0), tokens(1))
    }
    else {
      observeUnigram(ngram)
    }
  }

  def observeUnigram(word: String): Unit = {
    if(!wordsToInts.contains(word)) {
      wordsToInts.put(word, vocabSize)
      vocabSize += 1
    }

    val wordID = wordsToInts(word)
    unigramCounts.put(wordID, unigramCounts(wordID) + 1)    

    totalUnigramCount += 1
  }

  def observeBigram(word1: String, word2: String): Unit = {

    if(!wordsToInts.contains(word1)) {
      wordsToInts.put(word1, vocabSize)
      vocabSize += 1
    }
    if(!wordsToInts.contains(word2)) {
      wordsToInts.put(word2, vocabSize)
      vocabSize += 1
    }

    val idPair = (wordsToInts(word1), wordsToInts(word2))

    bigramCounts.put(idPair, bigramCounts(idPair) + 1)
  }

  def getNgramProb(ngram: String): Double = {
    if(ngram.contains(" ")) {
      val tokens = ngram.split(" ")
      getBigramProb(tokens(0), tokens(1))
    }
    else
      getUnigramProb(ngram)
  }

  def getUnigramProb(word: String): Double = {
    unigramCounts(wordsToInts(word)).toDouble / totalUnigramCount
  }

  def getBigramProb(word1: String, word2: String): Double = {
    bigramCounts((wordsToInts(word1), wordsToInts(word2))).toDouble / totalUnigramCount
  }

  def size = totalUnigramCount
}
