import java.io.File

import com.itv.scalapact.plugin._

name := "consumer"

scalaVersion := "2.13.6"

enablePlugins(ScalaPactPlugin)

lazy val pactVersionFile: SettingKey[File] = settingKey[File]("location of scala-pact version for examples")
pactVersionFile := baseDirectory.value.getParentFile.getParentFile / "version.sbt"

libraryDependencies ++= {
  //A hack so we don't have to manually update the scala-pact version for the examples
  lazy val pactVersion = IO.read(pactVersionFile.value).split('"')(1)
  Seq(
    "com.itv"       %% "scalapact-scalatest-suite" % pactVersion % "test",
    "org.scalatest" %% "scalatest"                 % "3.2.9"     % "test"
  )
}

scalaPactEnv := ScalaPactEnv.defaults.withPort(8080)
