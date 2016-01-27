
name := "scala-pact-test"

organization := "none"

version := "1.0.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.typesafe" % "config" % "1.2.1",
  "org.scalaj" %% "scalaj-http" % "1.1.5",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.scalaz" %% "scalaz-core" % "7.1.5",
  "com.github.tomakehurst" % "wiremock" % "1.57" % "test",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "com.github.kristofa" % "mock-http-server" % "4.1"
)
