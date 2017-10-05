# Scala-Pact
A Consumer Driven Contract testing library for Scala and [ScalaTest](http://www.scalatest.org/) that follows the [Pact](https://docs.pact.io/) standard.

Scala-Pact is intended for Scala developers who are looking for a better way to manage the HTTP contracts between their services.

## Latest version is 2.2.0

To get started with SBT:

Add the following lines to your `build.sbt` file to setup the test framework:
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

Add these lines to your `project/plugins.sbt` file to install the plugin:
```scala
libraryDependencies ++= Seq(
  "com.itv" %% "scalapact-argonaut-6-2"  % "2.2.0",
  "com.itv" %% "scalapact-http4s-0-16-2" % "2.2.0"
)

addSbtPlugin("com.itv" % "sbt-scalapact" % "2.2.0")
```

Please visit our [official documentation site](http://io.itv.com/scala-pact/) for more details and examples.

There is also an [example project](http://io.itv.com/scala-pact/examples/index.html) setup for reference.
