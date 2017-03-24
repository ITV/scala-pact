name := "scalapact-docs"

organization := "com.itv"

version := "2.1.4-SNAPSHOT"

scalaVersion := "2.12.1"

enablePlugins(ParadoxSitePlugin)

sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox"

lazy val root = (project in file(".")).
  enablePlugins(ParadoxPlugin).
  settings(
    name := "Scala-Pact Docs",
    paradoxTheme := Some(builtinParadoxTheme("generic"))
  )

ghpages.settings

git.remoteRepo := "git@github.com:ITV/scala-pact.git"
