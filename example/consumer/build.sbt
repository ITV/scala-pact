import java.io.File

import com.itv.scalapact.plugin._

name := "consumer"

organization := "com.example"

scalaVersion := "2.12.10"

enablePlugins(ScalaPactPlugin)

lazy val pactVersion = IO.read(new File("../version.txt"))

libraryDependencies ++=
  Seq(
    "com.itv"       %% "scalapact-circe-0-9"   % pactVersion % "test",
    "com.itv"       %% "scalapact-http4s-0-18" % pactVersion % "test",
    "com.itv"       %% "scalapact-scalatest"   % pactVersion % "test",
    "org.scalaj"    %% "scalaj-http"           % "2.3.0",
    "org.slf4j"     % "slf4j-simple"           % "1.6.4",
    "org.json4s"    %% "json4s-native"         % "3.5.0",
    "org.scalatest" %% "scalatest"             % "3.0.1" % "test"
  )

scalaPactEnv := ScalaPactEnv.defaults.withPort(8080)
