# Setup guide

Scala-Pact is comprised of two parts:

1. The test framework - used for writing CDC tests in ScalaTest
1. The plugin - used for running Scala-Pact's tasks and commands

As of version 2.2.0, both the test framework and the plugin require you to specify which versions of the Http and JSON libraries you require.

## ScalaTest library
Add the dependency to your `build.sbt` file like this:

```scala

import com.itv.scalapact.plugin._

enablePlugins(ScalaPactPlugin)

libraryDependencies ++= Seq(
  "com.itv"       %% "scalapact-argonaut-6-2"  % "2.2.0" % "test",
  "com.itv"       %% "scalapact-http4s-0-16-2" % "2.2.0" % "test",
  "com.itv"       %% "scalapact-scalatest"     % "2.2.0" % "test",
  "org.scalatest" %% "scalatest"               % "3.0.1" % "test"
)
```

## SBT plugin
Add the plugin to your `project/plugins.sbt` file like this:

```scala
libraryDependencies ++= Seq(
  "com.itv" %% "scalapact-argonaut-6-2"  % "2.2.0",
  "com.itv" %% "scalapact-http4s-0-16-2" % "2.2.0"
)

addSbtPlugin("com.itv" % "sbt-scalapact" % "2.2.0")
```

## Other pluggable dependency options
Please check the @ref:[compatibility matrix](project-deps.md) before using.

JSON Libraries
```
"com.itv" %% "scalapact-argonaut-6-1"  % "2.2.0"
"com.itv" %% "scalapact-argonaut-6-2"  % "2.2.0"
"com.itv" %% "scalapact-circe-0-8"     % "2.2.0"
```

HTTP Libraries
```
"com.itv" %% "scalapact-http4s-0-15-0a"  % "2.2.0"
"com.itv" %% "scalapact-http4s-0-16-2"   % "2.2.0"
"com.itv" %% "scalapact-http4s-0-16-2a"  % "2.2.0"
"com.itv" %% "scalapact-http4s-0-17-0"   % "2.2.0"
```
