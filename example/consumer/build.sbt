import java.io.File

import com.itv.scalapact.plugin._

name := "consumer"

organization := "com.example"

scalaVersion := "2.13.6"

enablePlugins(ScalaPactPlugin)

lazy val pactVersionFile: SettingKey[File] = settingKey[File]("location of scala-pact version for examples")
pactVersionFile := baseDirectory.value.getParentFile.getParentFile / "version.sbt"

libraryDependencies ++= {
  //A hack so we don't have to manually update the scala-pact version for the examples
  lazy val pactVersion = IO.read(pactVersionFile.value).split('"')(1)
  Seq(
    "com.itv"       %% "scalapact-circe-0-14"  % pactVersion % "test",
    "com.itv"       %% "scalapact-http4s-0-23" % pactVersion % "test",
    "com.itv"       %% "scalapact-scalatest"   % pactVersion % "test",
    "org.scalaj"    %% "scalaj-http"           % "2.4.2",
    "org.slf4j"      % "slf4j-simple"          % "1.7.32",
    "org.json4s"    %% "json4s-native"         % "4.0.2",
    "org.scalatest" %% "scalatest"             % "3.2.9"     % "test"
  )
}

scalaPactEnv := ScalaPactEnv.defaults.withPort(8080)
