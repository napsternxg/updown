import sbt._

class UpdownProject (info: ProjectInfo) extends DefaultProject(info) /*with assembly.AssemblyBuilder*/ {
  override def disableCrossPaths = true 
  //val argot = "org.clapper" %% "argot" % "0.3.1"
}


