name := "website"

organization := "com.itv"

version := "2.0.1-SNAPSHOT"

scalaVersion := "2.12.1"

initialCommands := "import com.itv.website._"

enablePlugins(ParadoxSitePlugin)

sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox"

lazy val root = (project in file(".")).
  enablePlugins(ParadoxPlugin).
  settings(
    name := "Scala-Pact Docs",
    siteSubdirName in Paradox := "",
    paradoxTheme := Some(builtinParadoxTheme("generic"))
  )

ghpages.settings

git.remoteRepo := "git@github.com:ITV/scala-pact.git"
