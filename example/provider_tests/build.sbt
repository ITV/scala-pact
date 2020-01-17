organization := "com.example"

name := "provider_tests"

version := "0.0.1"

scalaVersion := "2.12.10"

libraryDependencies ++=
  Seq(
    "org.http4s"    %% "http4s-blaze-server"   % "0.18.9",
    "org.http4s"    %% "http4s-dsl"            % "0.18.9",
    "org.http4s"    %% "http4s-circe"          % "0.18.9",
    "org.slf4j"     % "slf4j-simple"           % "1.6.4",
    "org.scalatest" %% "scalatest"             % "3.0.1" % "test",
    "com.itv"       %% "scalapact-circe-0-9"   % "2.3.13" % "test",
    "com.itv"       %% "scalapact-http4s-0-18" % "2.3.13" % "test",
    "com.itv"       %% "scalapact-scalatest"   % "2.3.13" % "test",
    // Optional for auto-derivation of JSON codecs
    "io.circe" %% "circe-generic" % "0.9.0",
    // Optional for string interpolation to JSON model
    "io.circe" %% "circe-literal" % "0.9.0"
  )
