
lazy val commonSettings = Seq(
  version := "2.2.0-SNAPSHOT",
  organization := "com.itv"
)

lazy val core =
  (project in file("scalapact-core"))
    .settings(commonSettings: _*).cross

lazy val core_2_10 = core("2.10.6")
lazy val core_2_11 = core("2.11.8")
lazy val core_2_12 = core("2.12.1")

lazy val plugin =
  (project in file("scalapact-sbtplugin"))
    .settings(commonSettings: _*)
    .dependsOn(core_2_10)
    .settings(
      sbtPlugin := true,
      scalaVersion := "2.10.6"
    )

lazy val framework =
  (project in file("scalapact-scalatest"))
    .settings(commonSettings: _*).cross

lazy val framework_2_11 = framework("2.11.8").dependsOn(core_2_11)
lazy val framework_2_12 = framework("2.12.1").dependsOn(core_2_12)

lazy val standalone =
  (project in file("scalapact-standalone-stubber"))
    .settings(commonSettings: _*)
    .dependsOn(core_2_12)
    .settings(
      name := "scalapact-standalone-stubber",
      scalaVersion := "2.12.1"
    )

lazy val docs =
  (project in file("scalapact-docs"))
    .settings(commonSettings: _*)
    .enablePlugins(ParadoxPlugin).
    settings(
      name := "Scala-Pact Docs",
      crossScalaVersions := Seq("2.12.1"),
      paradoxTheme := Some(builtinParadoxTheme("generic"))
    )

lazy val scalaPactProject =
  (project in file("."))
    .settings(commonSettings: _*)
    .aggregate(core_2_10, core_2_11, core_2_12, plugin, framework_2_11, framework_2_12, standalone, docs)
