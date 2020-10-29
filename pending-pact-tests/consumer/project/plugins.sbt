import java.io.File

import sbt.Defaults.sbtPluginExtra

/*
This is just so we don't need to set the version manually for the examples. In reality you should just do

addSbtPlugin("com.itv" % "sbt-scalapact" % scalaPactVersion)
*/

lazy val pactVersionFile: SettingKey[File] = settingKey[File]("location of scala-pact version for examples")
pactVersionFile := baseDirectory.value.getParentFile.getParentFile.getParentFile / "version.sbt"

libraryDependencies += {
  val pactVersion = IO.read(pactVersionFile.value).split('"')(1)
  val sbtV = (sbtBinaryVersion in pluginCrossBuild).value
  val scalaV = (scalaBinaryVersion in update).value
  sbtPluginExtra("com.itv" % "sbt-scalapact" % pactVersion, sbtV, scalaV)
}
