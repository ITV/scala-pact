
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

addCommandAlias("quickcompile", ";shared_2_12/compile;core_2_12/compile;argonaut62_2_12/compile;pactSpec_2_12/compile;plugin/compile;standalone/compile;framework_2_12/compile")
addCommandAlias("quicktest", ";shared_2_12/test;core_2_12/test;argonaut62_2_12/test;pactSpec_2_12/test;plugin/test;standalone/test;framework_2_12/test")

lazy val commonSettings = Seq(
  version := "2.2.0-SNAPSHOT",
  organization := "com.itv",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  )
)

val scala210: String = "2.10.6"
val scala211: String = "2.11.8"
val scala212: String = "2.12.1"

lazy val shared =
  (project in file("scalapact-shared"))
    .settings(commonSettings: _*).cross

lazy val shared_2_10 = shared(scala210)
lazy val shared_2_11 = shared(scala211)
lazy val shared_2_12 = shared(scala212)

lazy val core =
  (project in file("scalapact-core"))
    .settings(commonSettings: _*).cross

lazy val core_2_10 = core(scala210)
  .dependsOn(shared_2_10)
  .dependsOn(argonaut62_2_10 % "provided")
  .dependsOn(http4s0150a_2_10 % "provided")
  .project
lazy val core_2_11 = core(scala211)
  .settings(
    libraryDependencies ++= Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.6")
  )
  .dependsOn(shared_2_11)
  .dependsOn(argonaut62_2_11 % "provided")
  .dependsOn(http4s0150a_2_11 % "provided")
  .project
lazy val core_2_12 = core(scala212)
  .settings(
    libraryDependencies ++= Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.6")
  )
  .dependsOn(shared_2_12)
  .dependsOn(argonaut62_2_12 % "provided")
  .dependsOn(http4s0150a_2_12 % "provided")
  .project

lazy val http4s0150a =
  (project in file("scalapact-http4s-0-15-0a"))
    .settings(commonSettings: _*).cross

lazy val http4s0150a_2_10 = http4s0150a(scala210).dependsOn(shared_2_10)
lazy val http4s0150a_2_11 = http4s0150a(scala211).dependsOn(shared_2_11)
lazy val http4s0150a_2_12 = http4s0150a(scala212).dependsOn(shared_2_12)

lazy val argonaut62 =
  (project in file("scalapact-argonaut-6-2"))
    .settings(commonSettings: _*).cross

lazy val argonaut62_2_10 = argonaut62(scala210).dependsOn(shared_2_10)
lazy val argonaut62_2_11 = argonaut62(scala211).dependsOn(shared_2_11)
lazy val argonaut62_2_12 = argonaut62(scala212).dependsOn(shared_2_12)

lazy val argonaut61 =
  (project in file("scalapact-argonaut-6-1"))
    .settings(commonSettings: _*).cross

lazy val argonaut61_2_10 = argonaut61(scala210).dependsOn(shared_2_10)
lazy val argonaut61_2_11 = argonaut61(scala211).dependsOn(shared_2_11)
//lazy val argonaut61_2_12 = argonaut61(scala212).dependsOn(shared_2_12) // No such thing

lazy val circe08 =
  (project in file("scalapact-circe-0-8"))
    .settings(commonSettings: _*).cross

lazy val circe08_2_10 = circe08(scala210).dependsOn(shared_2_10).settings(
  addCompilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
  )
)
lazy val circe08_2_11 = circe08(scala211).dependsOn(shared_2_11)
lazy val circe08_2_12 = circe08(scala212).dependsOn(shared_2_12)

lazy val pactSpec =
  (project in file("pact-spec-tests"))
    .settings(commonSettings: _*).cross

lazy val pactSpec_2_10 = pactSpec(scala210).dependsOn(core_2_10, argonaut62_2_10)
lazy val pactSpec_2_11 = pactSpec(scala211).dependsOn(core_2_11, argonaut62_2_11)
lazy val pactSpec_2_12 = pactSpec(scala212).dependsOn(core_2_12, argonaut62_2_12)

lazy val plugin =
  (project in file("scalapact-sbtplugin"))
    .settings(commonSettings: _*)
    .dependsOn(core_2_10)
    .dependsOn(argonaut62_2_10 % "provided")
    .dependsOn(http4s0150a_2_10 % "provided")
    .project
    .settings(
      sbtPlugin := true,
      scalaVersion := scala210
    )

lazy val framework =
  (project in file("scalapact-scalatest"))
    .settings(commonSettings: _*)
    .cross

lazy val framework_2_11 =
  framework(scala211)
    .dependsOn(core_2_11)
    .dependsOn(argonaut62_2_11 % "provided")
    .dependsOn(http4s0150a_2_11 % "provided")
    .project
lazy val framework_2_12 =
  framework(scala212)
    .dependsOn(core_2_12)
    .dependsOn(argonaut62_2_12 % "provided")
    .dependsOn(http4s0150a_2_12 % "provided")
    .project

lazy val standalone =
  (project in file("scalapact-standalone-stubber"))
    .settings(commonSettings: _*)
    .dependsOn(core_2_12)
    .dependsOn(argonaut62_2_12)
    .dependsOn(http4s0150a_2_12)
    .settings(
      name := "scalapact-standalone-stubber",
      scalaVersion := scala212
    )

lazy val docs =
  (project in file("scalapact-docs"))
    .settings(commonSettings: _*)
    .enablePlugins(ParadoxPlugin).
    settings(
      name := "Scala-Pact Docs",
      crossScalaVersions := Seq(scala212),
      paradoxTheme := Some(builtinParadoxTheme("generic"))
    )

lazy val scalaPactProject =
  (project in file("."))
    .settings(commonSettings: _*)
    .aggregate(
      core_2_10,
      core_2_11,
      core_2_12,
      plugin,
      framework_2_11,
      framework_2_12,
      standalone,
      docs,
      http4s0150a_2_10,
      http4s0150a_2_11,
      http4s0150a_2_12,
      argonaut61_2_10,
      argonaut61_2_11,
      argonaut62_2_10,
      argonaut62_2_11,
      argonaut62_2_12,
      circe08_2_10,
      circe08_2_11,
      circe08_2_12,
      pactSpec_2_10,
      pactSpec_2_11,
      pactSpec_2_12
    )
