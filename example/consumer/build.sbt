import com.itv.scalapact.plugin._

name := "consumer"

organization := "com.example"

scalaVersion := "2.12.5"

version := "0.0.1"

enablePlugins(ScalaPactPlugin)

libraryDependencies ++=
  Seq(
    "com.itv"       %% "scalapact-circe-0-9"   % "2.3.9-SNAPSHOT" % "test",
    "com.itv"       %% "scalapact-http4s-0-18" % "2.3.9-SNAPSHOT" % "test",
    "com.itv"       %% "scalapact-scalatest"   % "2.3.9-SNAPSHOT" % "test",
    "org.scalaj"    %% "scalaj-http"           % "2.3.0",
    "org.slf4j"     % "slf4j-simple"           % "1.6.4",
    "org.json4s"    %% "json4s-native"         % "3.5.0",
    "org.scalatest" %% "scalatest"             % "3.0.1" % "test"
  )

scalaPactEnv := ScalaPactEnv.defaults.withPort(8080)
