import sbt._
import Keys._

object Build extends sbt.Build with BuildExtra {
  lazy val sbtIdea = Project("scalapact-plugin", file("."), settings = mainSettings)

  lazy val mainSettings: Seq[Def.Setting[_]] = Seq(
    sbtPlugin := true,
    organization := "com.itv.plugins",
    name := "scalapact-plugin",
    version := "2.0.1-SNAPSHOT",
    sbtVersion in Global := "0.13.13",
    scalaVersion in Global := "2.10.6",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    libraryDependencies <++= version { scalapactVersion =>
      Seq(
        "com.itv" %% "scalapact-core" % scalapactVersion
      )
    },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
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
  )
}
