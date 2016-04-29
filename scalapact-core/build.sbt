name := "scalapact-core"

organization := "com.itv"

version := "1.0.0-M3-SNAPSHOT"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.6", "2.11.7")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalaz" %% "scalaz-core" % "7.2.2",
  "io.argonaut" %% "argonaut" % "6.2-M1" withSources() withJavadoc()
)

publishTo := {
  val artifactory = "https://itvrepos.artifactoryonline.com/itvrepos/oasvc-ivy"
  if (isSnapshot.value)
    Some("Artifactory Realm" at artifactory)
  else
    Some("Artifactory Realm" at artifactory + ";build.timestamp=" + new java.util.Date().getTime)
}
