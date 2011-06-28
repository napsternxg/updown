package updown.util

object StringUtil {
  def stripPunc(s: String): String = {
    var toReturn = s.trim
    while(toReturn.length > 0 && !Character.isLetterOrDigit(toReturn.charAt(0)))
      toReturn = toReturn.substring(1)
    while(toReturn.length > 0 && !Character.isLetterOrDigit(toReturn.charAt(toReturn.length-1)))
      toReturn = toReturn.substring(0,toReturn.length()-1)
    toReturn
  }

  def preprocess(s: String): String = {
    val strippedToken = stripPunc(s).toLowerCase
    if(strippedToken.length > 0)
      return strippedToken
    return s
  }

  def stripPuncKeepHash(s: String): String = {
    var toReturn = s.trim
    while(toReturn.length > 0 && !Character.isLetterOrDigit(toReturn.charAt(0))
        && !(toReturn.length >= 2 && toReturn.charAt(0) == '#' && Character.isLetterOrDigit(toReturn.charAt(1))))
      toReturn = toReturn.substring(1)
    while(toReturn.length > 0 && !Character.isLetterOrDigit(toReturn.charAt(toReturn.length-1)))
      toReturn = toReturn.substring(0,toReturn.length()-1)
    toReturn
  }

  def preprocessKeepHash(s: String): String = {
    val strippedToken = stripPuncKeepHash(s).toLowerCase
    if(strippedToken.length > 0)
      return strippedToken
    return s
  }
}
