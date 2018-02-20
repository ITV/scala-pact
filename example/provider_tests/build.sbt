organization := "com.example"

name := "provider_tests"

version := "0.0.1"

scalaVersion := "2.11.8"

libraryDependencies ++=
  Seq(
    "org.http4s"    %% "http4s-blaze-server" % "0.18.0",
    "org.http4s"    %% "http4s-dsl" % "0.18.0",
    "org.http4s"    %% "http4s-circe" % "0.18.0",
    "org.slf4j"     % "slf4j-simple" % "1.6.4",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.itv"       %% "scalapact-circe-0-9" % "2.2.4-SNAPSHOT" % "test",
    "com.itv"       %% "scalapact-http4s-0-18-0" % "2.2.4-SNAPSHOT" % "test",
    "com.itv"       %% "scalapact-scalatest" % "2.2.4-SNAPSHOT" % "test",
    // Optional for auto-derivation of JSON codecs
    "io.circe"      %% "circe-generic" % "0.9.0",
    // Optional for string interpolation to JSON model
    "io.circe"      %% "circe-literal" % "0.9.0"
  )
