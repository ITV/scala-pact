name := "website"

organization := "com.itv"

version := "1.0.5-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test" withSources() withJavadoc(),
  "org.scalacheck" %% "scalacheck" % "1.12.1" % "test" withSources() withJavadoc()
)

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
