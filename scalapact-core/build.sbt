name := "scalapact-core"

organization := "com.itv"

version := "0.0.1"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "io.argonaut" %% "argonaut" % "6.1" withSources() withJavadoc()
)

