import java.io.File

import sbt.Defaults.sbtPluginExtra

lazy val pactVersionFile: SettingKey[File] = settingKey[File]("location of scala-pact version for examples")
pactVersionFile := baseDirectory.value.getParentFile.getParentFile / "version.txt"

libraryDependencies += {
  val pactVersion = IO.read(pactVersionFile.value)
  val sbtV = (sbtBinaryVersion in pluginCrossBuild).value
  val scalaV = (scalaBinaryVersion in update).value
  sbtPluginExtra("com.itv" % "sbt-scalapact" % pactVersion, sbtV, scalaV)
}
