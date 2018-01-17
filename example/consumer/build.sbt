
import com.itv.scalapact.plugin._

name := "consumer"

organization := "com.example"

scalaVersion := "2.12.1"

version := "0.0.1"

enablePlugins(ScalaPactPlugin)

libraryDependencies ++=
  Seq(
    "com.itv"       %% "scalapact-argonaut-6-2"   % "2.2.2" % "test",
    "com.itv"       %% "scalapact-http4s-0-15-0a" % "2.2.2" % "test",
    "com.itv"       %% "scalapact-scalatest"      % "2.2.2" % "test",
    "org.scalaj"    %% "scalaj-http"              % "2.3.0",
    "org.slf4j"     %  "slf4j-simple"             % "1.6.4",
    "org.json4s"    %% "json4s-native"            % "3.5.0",
    "org.scalatest" %% "scalatest"                % "3.0.1"          % "test"
  )

scalaPactEnv := ScalaPactEnv.default.withPort(8080)
