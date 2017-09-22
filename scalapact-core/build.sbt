name := "scalapact-core"

// Pinched shamelessly from https://tpolecat.github.io/2014/04/11/scalac-flags.html
scalacOptions ++= Seq(
//  "-Yno-imports", // Powerful but boring. Essentially you have to pull in everything... one day.
  "-deprecation",
  "-encoding", "UTF-8",       // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked"
//  "-Xfatal-warnings",
//  "-Xlint",
//  "-Yno-adapted-args",
//  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
//  "-Ywarn-numeric-widen",
//  "-Ywarn-value-discard",
//  "-Xfuture"
)

lazy val http4sVersion = "0.15.0a"

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.2",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-dsl"          % http4sVersion
)

//wartremoverWarnings ++= Warts.unsafe

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
