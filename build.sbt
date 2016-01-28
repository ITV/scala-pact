
name := "ScalaPact"

organization := "com.itv"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.github.kristofa" % "mock-http-server" % "4.1",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "org.scalaj" %% "scalaj-http" % "1.1.5"
)
