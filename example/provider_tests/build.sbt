organization := "com.example"

name := "provider_tests"

version := "0.0.1"

scalaVersion := "2.13.1"

libraryDependencies ++=
  Seq(
    "org.http4s"    %% "http4s-blaze-server"   % "0.21.7",
    "org.http4s"    %% "http4s-dsl"            % "0.21.7",
    "org.http4s"    %% "http4s-circe"          % "0.21.7",
    "org.slf4j"     % "slf4j-simple"           % "1.6.4",
    "org.scalatest" %% "scalatest"             % "3.0.8" % "test",
    "com.itv"       %% "scalapact-circe-0-13"   % "2.3.19-SNAPSHOT" % "test",
    "com.itv"       %% "scalapact-http4s-0-21" % "2.3.19-SNAPSHOT" % "test",
    "com.itv"       %% "scalapact-scalatest"   % "2.3.19-SNAPSHOT" % "test",
    // Optional for auto-derivation of JSON codecs
    "io.circe" %% "circe-generic" % "0.13.0",
    // Optional for string interpolation to JSON model
    "io.circe" %% "circe-literal" % "0.13.0",
    "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.35.2" % "test"
  )
