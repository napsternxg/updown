name := "Updown"

version := "0.1.0"

organization := "OpenNLP"

scalaVersion := "2.9.1"

crossPaths := false

retrieveManaged := true

libraryDependencies ++= Seq(
  "org.apache.opennlp" % "opennlp-maxent" % "3.0.1-incubating",
  "org.clapper" %% "argot" % "0.3.5",
  "com.weiglewilczek.slf4s" %% "slf4s" % "1.0.7",
  "org.scalatest" %% "scalatest" % "1.6.1" % "test"
  )

// append several options to the list of options passed to the Java compiler
javacOptions ++= Seq("-Xlint")

// append -deprecation to the options passed to the Scala compiler
scalacOptions ++= Seq("-deprecation", "-Xlint")
