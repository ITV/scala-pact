# Setup guide

Scala-Pact is comprised of two parts:
1. The test framework - used for writing CDC tests in ScalaTest
1. The plugin - used for running Scala-Pact's tasks and commands

## ScalaTest library
Add the dependency to your `build.sbt` file like this:

```
libraryDependencies ++= Seq(
  "com.itv" %% "scalapact-scalatest" % "2.0.0" % "test"
)
```

## SBT plugin
Add the plugin to your `project/plugins.sbt` file like this:

```
addSbtPlugin("com.itv.plugins" % "scalapact-plugin" % "2.0.0")
```
