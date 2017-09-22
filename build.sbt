
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

lazy val commonSettings = Seq(
  version := "2.2.0-SNAPSHOT",
  organization := "com.itv"
)

val scala210: String = "2.10.6"
val scala211: String = "2.11.8"
val scala212: String = "2.12.1"

lazy val core =
  (project in file("scalapact-core"))
    .settings(commonSettings: _*).cross

lazy val core_2_10 = core(scala210)
lazy val core_2_11 = core(scala211)
lazy val core_2_12 = core(scala212)

lazy val argonaut62 =
  (project in file("scalapact-argonaut-6.2"))
    .settings(commonSettings: _*).cross

lazy val argonaut62_2_10 = argonaut62(scala210).dependsOn(core_2_10)
lazy val argonaut62_2_11 = argonaut62(scala211).dependsOn(core_2_11)
lazy val argonaut62_2_12 = argonaut62(scala212).dependsOn(core_2_12)

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
    .settings(
      sbtPlugin := true,
      scalaVersion := scala210
    )

lazy val framework =
  (project in file("scalapact-scalatest"))
    .settings(commonSettings: _*).cross

lazy val framework_2_11 = framework(scala211).dependsOn(core_2_11)
lazy val framework_2_12 = framework(scala212).dependsOn(core_2_12)

lazy val standalone =
  (project in file("scalapact-standalone-stubber"))
    .settings(commonSettings: _*)
    .dependsOn(core_2_12)
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
      argonaut62_2_10,
      argonaut62_2_11,
      argonaut62_2_12,
      pactSpec_2_10,
      pactSpec_2_11,
      pactSpec_2_12
    )
