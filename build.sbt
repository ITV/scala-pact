lazy val scalaVersion212: String = "2.12.14"
lazy val scalaVersion213: String = "2.13.6"
lazy val scalaVersion3: String   = "3.0.1"
lazy val supportedScalaVersions  = List(scalaVersion212, scalaVersion213)

ThisBuild / scalaVersion := scalaVersion212

lazy val commonSettings = Seq(
  organization := "com.itv",
  crossScalaVersions := supportedScalaVersions,
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.9" % "test"
  ),
  parallelExecution in Test := false,
//  javaOptions in Test ++= Seq(
//    "-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder"
//  ),
  test in assembly := {}
)

lazy val scala212OnlySettings = Seq(
  crossScalaVersions := Seq(scalaVersion212)
)

lazy val scala3Settings = Seq(
  crossScalaVersions += scalaVersion3,
  scalacOptions -= "-Xfatal-warnings",
  scalacOptions ++= Seq("-source:3.0-migration", "-rewrite")
)

lazy val mockSettings = Seq(
  libraryDependencies += "org.scalamock" %% "scalamock" % "4.0.0" % Test
)

lazy val publishSettings = Seq(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  pomExtra :=
    <url>https://github.com/ITV/scala-pact</url>
      <licenses>
        <license>
          <name>ITV-OSS</name>
          <url>http://itv.com/itv-oss-licence-v1.0</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <developers>
        <developer>
          <id>davesmith00000</id>
          <name>David Smith</name>
          <organization>ITV</organization>
          <organizationUrl>http://www.itv.com</organizationUrl>
        </developer>
      </developers>
)

lazy val shared =
  (project in file("scalapact-shared"))
    .enablePlugins(BuildInfoPlugin)
    .settings(
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "com.itv.scalapact.shared"
    )
    .settings(commonSettings: _*)
    .settings(scala3Settings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-shared",
      libraryDependencies ++= Seq("org.scala-lang.modules" %% "scala-xml" % "2.0.1")
    )

lazy val core =
  (project in file("scalapact-core"))
    .settings(commonSettings: _*)
    .settings(scala3Settings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-core"
    )
    .dependsOn(shared)

lazy val http4s021 =
  (project in file("scalapact-http4s-0-21"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-http4s-0-21",
      libraryDependencies ++= Seq(
        "org.http4s"            %% "http4s-blaze-server" % "0.21.26" exclude ("org.scala-lang.modules", "scala-xml"),
        "org.http4s"            %% "http4s-blaze-client" % "0.21.26" exclude ("org.scala-lang.modules", "scala-xml"),
        "org.http4s"            %% "http4s-dsl"          % "0.21.26",
        "com.github.tomakehurst" % "wiremock"            % "2.27.2" % "test"
      )
    )
    .dependsOn(shared)

lazy val http4s023 =
  (project in file("scalapact-http4s-0-23"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(scala3Settings: _*)
    .settings(
      name := "scalapact-http4s-0-23",
      libraryDependencies ++= Seq(
        "org.http4s"            %% "http4s-blaze-server" % "0.23.1" exclude ("org.scala-lang.modules", "scala-xml"),
        "org.http4s"            %% "http4s-blaze-client" % "0.23.1" exclude ("org.scala-lang.modules", "scala-xml"),
        "org.http4s"            %% "http4s-dsl"          % "0.23.1",
        "com.github.tomakehurst" % "wiremock"            % "2.27.2" % "test"
      )
    )
    .dependsOn(shared)

lazy val testShared =
  (project in file("scalapact-test-shared"))
    .settings(commonSettings: _*)
    .settings(scala3Settings: _*)
    .settings(
      name := "scalapact-test-shared",
      skip in publish := true
    )
    .dependsOn(shared)

lazy val argonaut62 =
  (project in file("scalapact-argonaut-6-2"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-argonaut-6-2",
      libraryDependencies ++= Seq(
        "io.argonaut" %% "argonaut" % "6.2.5"
      )
    )
    .dependsOn(shared)
    .dependsOn(testShared % "test->compile")

lazy val circe13 =
  (project in file("scalapact-circe-0-13"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-circe-0-13",
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-core",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-parser"
      ).map(_ % "0.13.0")
    )
    .dependsOn(shared)
    .dependsOn(testShared % "test->compile")

lazy val circe14 =
  (project in file("scalapact-circe-0-14"))
    .settings(commonSettings: _*)
    .settings(scala3Settings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-circe-0-14",
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-core",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-parser"
      ).map(_ % "0.14.1")
    )
    .dependsOn(shared)
    .dependsOn(testShared % "test->compile")

lazy val pluginShared =
  (project in file("sbt-scalapact-shared"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "sbt-scalapact-shared"
    )
    .dependsOn(core)
    .settings(scala212OnlySettings)

lazy val plugin =
  (project in file("sbt-scalapact"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "sbt-scalapact",
      sbtPlugin := true
    )
    .dependsOn(pluginShared)
    .dependsOn(circe14)
    .dependsOn(http4s023)
    .settings(scala212OnlySettings)

lazy val pluginNoDeps =
  (project in file("sbt-scalapact-nodeps"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "sbt-scalapact-nodeps",
      sbtPlugin := true
    )
    .dependsOn(pluginShared)
    .dependsOn(circe14 % "provided")
    .dependsOn(http4s023 % "provided")
    .settings(scala212OnlySettings)

lazy val framework =
  (project in file("scalapact-scalatest"))
    .settings(commonSettings: _*)
    .settings(scala3Settings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-scalatest",
      mappings in (Compile, packageBin) ~= {
        _.filterNot { case (_, fileName) => fileName == "logback.xml" || fileName == "log4j.properties" }
      }
    )
    .dependsOn(core)

lazy val frameworkWithDeps =
  (project in file("scalapact-scalatest-suite"))
    .settings(commonSettings: _*)
    .settings(scala3Settings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-scalatest-suite",
      mappings in (Compile, packageBin) ~= {
        _.filterNot { case (_, fileName) => fileName == "logback.xml" || fileName == "log4j.properties" }
      }
    )
    .dependsOn(framework)
    .dependsOn(circe14)
    .dependsOn(http4s023)

lazy val standalone =
  (project in file("scalapact-standalone-stubber"))
    .settings(commonSettings: _*)
    .settings(
      name := "scalapact-standalone-stubber",
      publish := {},
      assemblyJarName in assembly := "pactstubber.jar",
      libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % "1.2.5"
      ),
      skip in publish := true
    )
    .dependsOn(core)
    .dependsOn(circe13)
    .dependsOn(http4s021)

lazy val pactSpec =
  (project in file("pact-spec-tests"))
    .settings(commonSettings: _*)
    .settings(
      name := "pact-spec-tests",
      skip in publish := true
    )
    .settings(scala212OnlySettings)
    .dependsOn(core)
    .dependsOn(argonaut62)

lazy val testsWithDeps =
  (project in file("tests-with-deps"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scalaj"            %% "scalaj-http"   % "2.4.2"  % "test",
        "org.json4s"            %% "json4s-native" % "3.6.11" % "test",
        "com.github.tomakehurst" % "wiremock"      % "2.27.2" % "test",
        "fr.hmil"               %% "roshttp"       % "2.1.0"  % "test",
        "io.argonaut"           %% "argonaut"      % "6.2.5"
      ),
      skip in publish := true
    )
    .settings(scala212OnlySettings)
    .dependsOn(framework)
    .dependsOn(circe14)
    .dependsOn(http4s023)

lazy val docs =
  (project in file("scalapact-docs"))
    .settings(commonSettings: _*)
    .enablePlugins(ParadoxPlugin)
    .enablePlugins(ParadoxSitePlugin)
    .enablePlugins(GhpagesPlugin)
    .settings(
      paradoxTheme := Some(builtinParadoxTheme("generic")),
      name := "scalapact-docs",
      git.remoteRepo := "git@github.com:ITV/scala-pact.git",
      sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox",
      skip in publish := true
    )
    .settings(scala212OnlySettings)

lazy val scalaPactProject =
  (project in file("."))
    .settings(commonSettings: _*)
    .settings(
      skip in publish := true,
      crossScalaVersions := Nil
    )
    .aggregate(shared, core, pluginShared, plugin, pluginNoDeps, framework, testShared)
    .aggregate(http4s021, http4s023)
    .aggregate(argonaut62, circe13, circe14)
    .aggregate(standalone, frameworkWithDeps)
    .aggregate(docs)
    .aggregate(pactSpec, testsWithDeps)

import ReleaseTransformations._
import sbtrelease.Utilities._
import sbtrelease.Vcs
import scala.sys.process.ProcessLogger

val readmeFileKey = settingKey[File]("The location of the readme")
readmeFileKey := baseDirectory.value / "README.md"

lazy val updateVersionsInReadme: ReleaseStep = { st: State =>
  st.get(ReleaseKeys.versions)
    .flatMap { case (newVersion, _) =>
      val readmeFile    = st.extract.get(readmeFileKey).getCanonicalFile
      val readmeLines   = IO.readLines(readmeFile)
      val versionPrefix = "## Latest version is "
      val currentVersion = readmeLines.collectFirst {
        case s if s.startsWith(versionPrefix) => s.replace(versionPrefix, "").dropWhile(_.isWhitespace)
      }
      currentVersion.map { cv =>
        IO.writeLines(readmeFile, readmeLines.map(_.replaceAll(cv, newVersion)))
      }
    }
    .getOrElse(())

  st
}

lazy val commitReadMeVersionBump: ReleaseStep = { st: State =>
  def vcs(st: State): Vcs =
    st.extract
      .get(releaseVcs)
      .getOrElse(sys.error("Aborting release. Working directory is not a repository of a recognized VCS."))
  val commitMessage: TaskKey[String] = releaseNextCommitMessage
  val log = new ProcessLogger {
    override def err(s: => String): Unit = st.log.info(s)
    override def out(s: => String): Unit = st.log.info(s)
    override def buffer[T](f: => T): T   = st.log.buffer(f)
  }
  val readmeFile = st.extract.get(readmeFileKey).getCanonicalFile
  val base       = vcs(st).baseDir.getCanonicalFile
  val sign       = st.extract.get(releaseVcsSign)
  val signOff    = st.extract.get(releaseVcsSignOff)
  val relativePathToReadme = IO
    .relativize(base, readmeFile)
    .getOrElse("Readme file [%s] is outside of this VCS repository with base directory [%s]!" format (readmeFile, base))

  vcs(st).add(relativePathToReadme) !! log

  val status = vcs(st).status.!!.trim

  val newState = if (status.nonEmpty) {
    val (state, msg) = st.extract.runTask(commitMessage, st)
    vcs(state).commit(msg, sign, signOff) ! log
    state
  } else {
    st
  }
  newState
}

releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  updateVersionsInReadme,
  commitReadMeVersionBump,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
