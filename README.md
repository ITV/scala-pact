# Scala-Pact
A library for generating Consumer Driven Contract files in Scala projects following the PACT standard using [ScalaTest](http://www.scalatest.org/). Includes supporting tools that use Pact files to verify and stub services.

Scala-Pact is intended for Scala developers who are looking for a better way to manage the HTTP contracts between their services.

## Latest version is 2.1.0

To get started with SBT:

Add the following line to you `build.sbt` file to setup the test framework:
```scala
libraryDependencies += "com.itv" %% "scalapact-scalatest" % "2.1.0"
```

Add this line to your `project/plugins.sbt` file to install the plugin:
```scala
addSbtPlugin("com.itv.plugins" % "scalapact-plugin" % "2.1.0")
```

Please visit our [official documentation site](http://io.itv.com/scala-pact/) for more details and examples.

There is also an [example project](http://io.itv.com/scala-pact/examples/index.html) setup for reference.
