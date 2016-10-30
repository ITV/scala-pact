name := "scalapact-core"

organization := "com.itv"

version := "1.0.6-SNAPSHOT"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.6", "2.11.7")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalaz" %% "scalaz-core" % "7.2.2",
  "io.argonaut" %% "argonaut" % "6.2-M1" withSources() withJavadoc(),
  "org.http4s" %% "http4s-blaze-server" % "0.13.2a" withSources() withJavadoc(),
  "org.http4s" %% "http4s-dsl"          % "0.13.2a" withSources() withJavadoc(),
  "org.http4s" %% "http4s-argonaut"     % "0.13.2a" withSources() withJavadoc(),
  "org.scalaj" %% "scalaj-http" % "2.2.1"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/ITV/scala-pact</url>
  <licenses>
    <license>
      <name>ITV-OSS</name>
      <url>http://itv.com/itv-oss-licence-v1.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:itv/scala-pact.git</url>
    <connection>scm:git:git@github.com:itv/scala-pact.git</connection>
  </scm>
  <developers>
    <developer>
      <id>davesmith00000</id>
      <name>David Smith</name>
      <organization>ITV</organization>
      <organizationUrl>http://www.itv.com</organizationUrl>
    </developer>
  </developers>
)
