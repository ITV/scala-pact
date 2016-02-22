import sbt._
import Keys._

object Build extends sbt.Build with BuildExtra {
  lazy val sbtIdea = Project("sbt-idea", file("."), settings = mainSettings)

  lazy val mainSettings: Seq[Project.Setting[_]] = Defaults.defaultSettings ++ Seq(
    sbtPlugin := true,
    organization := "com.itv.plugins",
    name := "scalapact-plugin",
    version := "0.1.0",
    sbtVersion in Global := "0.13.6",
    scalaVersion in Global := "2.10.6",
    publishMavenStyle := false,
    publishArtifact in Test := false,
    pomIncludeRepository := (_ => false),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-native" % "3.2.10"
    )
  )
}
