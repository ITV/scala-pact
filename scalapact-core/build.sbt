name := "scalapact-core"

organization := "com.itv"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.6", "2.11.7")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "io.argonaut" %% "argonaut" % "6.1" withSources() withJavadoc()
)


publishTo := Some("Artifactory Realm" at "https://itvrepos.artifactoryonline.com/itvrepos/cps-libs")
