

name := "sbt-scalapact"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

pomExtra :=
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
