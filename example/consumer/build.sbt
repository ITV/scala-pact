import com.itv.scalapact.plugin._

name := "consumer"

organization := "com.example"

scalaVersion := "2.13.1"

version := "0.0.1"

enablePlugins(ScalaPactPlugin)

libraryDependencies ++=
  Seq(
    "com.itv"       %% "scalapact-circe-0-13"   % "2.3.19-SNAPSHOT" % "test",
    "com.itv"       %% "scalapact-http4s-0-21" % "2.3.19-SNAPSHOT" % "test",
    "com.itv"       %% "scalapact-scalatest"   % "2.3.19-SNAPSHOT" % "test",
    "org.scalaj"    %% "scalaj-http"           % "2.4.2",
    "org.slf4j"     % "slf4j-simple"           % "1.6.4",
    "org.json4s"    %% "json4s-native"         % "3.6.9",
    "org.scalatest" %% "scalatest"             % "3.0.8" % "test"
  )

scalaPactEnv := ScalaPactEnv.defaults.withPort(8080)
