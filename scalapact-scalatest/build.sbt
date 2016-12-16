
name := "scalapact-scalatest"

organization := "com.itv"

version := "2.0.1-SNAPSHOT"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

crossScalaVersions := Seq("2.11.8", "2.12.1")

libraryDependencies <++= version { scalapactVersion =>
  Seq(
    "com.itv" %% "scalapact-core" % scalapactVersion,
    "org.scalaj" %% "scalaj-http" % "2.3.0" % "test",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "org.json4s" %% "json4s-native" % "3.5.0" % "test",
    "com.github.tomakehurst" % "wiremock" % "1.56" % "test"
  )
}

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
