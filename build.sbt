
lazy val commonSettings = Seq(
  version := "2.2.0-SNAPSHOT",
  organization := "com.itv"
)

lazy val core =
  (project in file("scalapact-core"))
    .settings(commonSettings: _*)
    .settings(
      crossScalaVersions := Seq("2.12.1", "2.11.8", "2.10.6")
    )

lazy val plugin =
  (project in file("scalapact-sbtplugin"))
    .settings(commonSettings: _*)
    .dependsOn(core)
    .settings(
      sbtPlugin := true,
      crossScalaVersions := Seq("2.10.6")
    )

lazy val framework =
  (project in file("scalapact-scalatest"))
    .settings(commonSettings: _*)
    .dependsOn(core)
    .settings(
      crossScalaVersions := Seq("2.12.1", "2.11.8")
    )

lazy val standalone =
  (project in file("scalapact-standalone-stubber"))
    .settings(commonSettings: _*)
    .dependsOn(core)
    .settings(
      name := "scalapact-standalone-stubber",
      crossScalaVersions := Seq("2.12.1")
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
    .aggregate(core, plugin, framework, standalone, docs)
