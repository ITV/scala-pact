name := "scalapact-standalone-stubber"

organization := "com.itv"

version := "1.0.2-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test" withSources() withJavadoc(),
  "com.itv" % "scalapact-core_2.11" % "1.0.2-SNAPSHOT"
)

