import sbt._
import reaktor.scct.ScctProject

class UpdownProject (info: ProjectInfo) extends DefaultProject(info)  with ScctProject/*with assembly.AssemblyBuilder*/ {
  override def disableCrossPaths = true 

  // Add repositories
  val gsonRepo = "gson repo" at "http://google-gson.googlecode.com/svn/mavenrepo"
  val opennlpRepo = "opennlp sourceforge repo" at "http://opennlp.sourceforge.net/maven2"

  // Dependencies
  val opennlpTools = "org.apache.opennlp" % "opennlp-tools" % "1.5.1-incubating"
  val opennlpMaxent = "org.apache.opennlp" % "opennlp-maxent" % "3.0.1-incubating"
  val argot = "org.clapper" %% "argot" % "0.3.5"
  val scalatest = "org.scalatest" % "scalatest_2.9.1" % "1.6.1"
}

