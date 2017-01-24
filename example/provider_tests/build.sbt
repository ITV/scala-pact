organization := "com.example"
name := "provider_tests"
version := "0.0.1"
scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "org.http4s"     %% "http4s-blaze-server" % "0.15.0a",
  "org.http4s"     %% "http4s-dsl"          % "0.15.0a",
  "org.http4s"     %% "http4s-argonaut"     % "0.15.0a",
  "org.scalatest"  %% "scalatest"           % "3.0.0" % "test",
  "com.itv"        %% "scalapact-scalatest" % "2.1.1" % "test"
)

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"
