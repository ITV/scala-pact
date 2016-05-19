name := "scalapact-core"

organization := "com.itv"

version := "1.0.0-M10-SNAPSHOT"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.6", "2.11.7")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalaz" %% "scalaz-core" % "7.2.2",
  "io.argonaut" %% "argonaut" % "6.2-M1" withSources() withJavadoc(),
  "org.slf4j" % "slf4j-simple" % "1.6.4" withSources() withJavadoc(),
  "org.http4s" %% "http4s-blaze-server" % "0.13.2a" withSources() withJavadoc(),
  "org.http4s" %% "http4s-dsl"          % "0.13.2a" withSources() withJavadoc(),
  "org.http4s" %% "http4s-argonaut"     % "0.13.2a" withSources() withJavadoc(),
  "org.scalaj" %% "scalaj-http" % "2.2.1"
)

publishTo := {
  val artifactory = "https://itvrepos.artifactoryonline.com/itvrepos/oasvc-ivy"
  if (isSnapshot.value)
    Some("Artifactory Realm" at artifactory)
  else
    Some("Artifactory Realm" at artifactory + ";build.timestamp=" + new java.util.Date().getTime)
}
