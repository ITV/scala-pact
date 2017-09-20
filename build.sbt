
lazy val commonSettings = Seq(
  version := "2.2.0-RC1-SNAPSHOT",
  scalaVersion := "2.12.1",
  organization := "com.itv"
)

lazy val core =
  (project in file("scalapact-core"))
    .settings(commonSettings: _*)

lazy val plugin =
  (project in file("scalapact-sbtplugin"))
    .settings(commonSettings: _*)
    .dependsOn(core)

lazy val framework =
  (project in file("scalapact-scalatest"))
    .settings(commonSettings: _*)
    .dependsOn(core)

lazy val standalone =
  (project in file("scalapact-standalone-stubber"))
    .settings(commonSettings: _*)
    .dependsOn(core)

// lazy val docs =
//   (project in file("scalapact-docs"))
//     .settings(commonSettings: _*)
//     .dependsOn(core)

lazy val scalaPactProject =
  (project in file("."))
    .settings(commonSettings: _*)
    .aggregate(core, plugin, framework, standalone)
