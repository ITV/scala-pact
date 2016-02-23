import sbt._
import Keys._

object Build extends sbt.Build with BuildExtra {
  lazy val sbtIdea = Project("scalapact-plugin", file("."), settings = mainSettings)

  lazy val mainSettings: Seq[Def.Setting[_]] = /*Defaults.defaultSettings ++*/ Seq(
    sbtPlugin := true,
    organization := "com.itv.plugins",
    name := "scalapact-plugin",
    version := "0.1.0-SNAPSHOT",
    sbtVersion in Global := "0.13.11",
    scalaVersion in Global := "2.10.6",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    libraryDependencies ++= Seq(
      "io.argonaut" %% "argonaut" % "6.1"
    )
  )
}
