# Scala-Pact [![Build Status](https://travis-ci.com/ITV/scala-pact.svg?branch=master)](https://travis-ci.com/ITV/scala-pact)

A Consumer Driven Contract testing library for Scala and [ScalaTest](http://www.scalatest.org/) that follows the [Pact](https://docs.pact.io/) standard.

Scala-Pact is intended for Scala developers who are looking for a better way to manage the HTTP contracts between their services.

If you are just starting out on your pact journey in scala, we recommend checking out [pact4s](https://github.com/jbwheatley/pact4s). This is built directly on top of pact-jvm, and provides support for writing and verifying contracts using [scalaTest](https://github.com/scalatest/scalatest), [weaver-test](https://github.com/disneystreaming/weaver-test), and [munit-cats-effect-3](https://github.com/typelevel/munit-cats-effect). 

## Latest version is 4.4.0

Scala-Pact currently only supports [v2 of the pact specification](https://github.com/pact-foundation/pact-specification/tree/version-2). Support for v3 is a future goal of the project. 

### Scala-Pact >= 3.0.x 

Before this version, the project versioning did not follow semantic versioning. From this point onwards, the version format will be `major.minor.patch`, and it should be noted that the `3` in the version number does *not* correspond to the version supporting v3 of the pact spec (for the time-being, at least).

Scala-Pact now has two branches based on SBT requirements. 

#### SBT 1.x compatible (Latest 4.4.0)

All development going forward begins at `2.3.x` and resides on the `master` branch.
For the sake of the maintainer's sanity, version 2.3.x and beyond will only support Scala 2.12 and SBT 1.x or greater. The project is currently cross-compiled across scala 2.12.12 and 2.13.4. 

#### SBT 0.13.x compatible (Latest 2.2.5)

The reluctantly maintained EOL maintenance version of Scala-Pact lives on a branch called `v2.2.x`.
These versions support Scala 2.10, 2.11, and 2.12 but are limited by only supporting SBT 0.13.x.

## More information

Please visit our [official documentation site](http://io.itv.com/scala-pact/) for more details and examples.

There is also an [example project](http://io.itv.com/scala-pact/examples/index.html) setup for reference.

## Getting setup

Scala-Pact goes to great lengths to help you avoid / work around dependency conflicts. 
This is achieved by splitting the core functionality out of the library requirements which are provided separately. This allows you to align or avoid conflicting dependencies e.g. If your project uses a specific version of Circe, tell Scala-Pact to use Argonaut!
One big change between 2.2.x and 2.3.x is that dependencies are now provided by TypeClass rather than just static linking. Please refer to the [example setup](https://github.com/ITV/scala-pact/tree/master/example).

### You're using SBT 1.x:

There are two approaches to using the scala-pact dsl. The new approach uses a single dependency and mix-ins to use the dsl. The old approach allows more freedom in which http and json library versions are being used, but requires more dependencies and imports. 

#### Mix-ins approach 

Add the following lines to your `build.sbt` file to setup the test framework:
```scala

import com.itv.scalapact.plugin._

enablePlugins(ScalaPactPlugin)
        
libraryDependencies ++= Seq(
  "com.itv"       %% "scalapact-scalatest-suite"   % "4.4.0" % "test",
  "org.scalatest" %% "scalatest"                   % "3.2.9"  % "test"
)
```

Add this line to your `project/plugins.sbt` file to install the plugin:
```scala
addSbtPlugin("com.itv" % "sbt-scalapact" % "4.4.0")
```

Both the import and the plugin come pre-packaged with the latest JSON and Http libraries (http4s 0.21.x, and circe 0.13.x). 

In your consumer test suites, have the test class extend `PactForgerSuite`. In your provider test suites, have the test class extend `PactVerifySuite`. 

#### Without mix-ins
If your project needs more control over the dependencies used by scala-pact, add the following lines to your `build.sbt` file to setup the test framework:
```scala

import com.itv.scalapact.plugin._

enablePlugins(ScalaPactPlugin)
        
libraryDependencies ++= Seq(
  "com.itv"       %% "scalapact-circe-0-13"   % "4.4.0" % "test",
  "com.itv"       %% "scalapact-http4s-0-21"  % "4.4.0" % "test",
  "com.itv"       %% "scalapact-scalatest"    % "4.4.0" % "test",
  "org.scalatest" %% "scalatest"              % "3.2.9" % "test"
)
```

Add this line to your `project/plugins.sbt` file to install the plugin:
```scala
addSbtPlugin("com.itv" % "sbt-scalapact" % "4.4.0")
```
This version of the plugin comes pre-packaged with the latest JSON and Http libraries.
Thanks to the way SBT works, that one plugin line will work in most cases, but if you're still having conflicts, you can also do this to use your preferred libraries:

```scala
 libraryDependencies ++= Seq(
   "com.itv" %% "scalapact-argonaut-6-2" % "4.4.0",
   "com.itv" %% "scalapact-http4s-0-21"  % "4.4.0"
 )
 
 addSbtPlugin("com.itv" % "sbt-scalapact-nodeps" % "4.4.0")
```

In your test suite, you will need the following imports:

The DSL/builder import for Consumer tests:
```scala
  import com.itv.scalapact.ScalaPactForger._
```
Or this one for Verification tests:
```scala
  import com.itv.scalapact.ScalaPactVerify._
``` 
You'll also need to reference the json and http libraries specified in the `build.sbt` file:
```scala
  import com.itv.scalapact.circe09._
  import com.itv.scalapact.http4s18._
```
Alternatively, in case your project has both `scalapact-http4s` and `scalapact-circe` as dependencies, you could also use the following:

```scala
  import com.itv.scalapact.json._
  import com.itv.scalapact.http._
```

### You're using SBT 0.13.x:

Add the following lines to your `build.sbt` file to setup the test framework:
```scala

import com.itv.scalapact.plugin._

enablePlugins(ScalaPactPlugin)
        
libraryDependencies ++= Seq(
  "com.itv"       %% "scalapact-circe-0-9"     % "2.2.5" % "test",
  "com.itv"       %% "scalapact-http4s-0-18-0" % "2.2.5" % "test",
  "com.itv"       %% "scalapact-scalatest"     % "2.2.5" % "test",
  "org.scalatest" %% "scalatest"               % "3.0.5" % "test"
)
```

Add these lines to your `project/plugins.sbt` file to install the plugin:
```scala
libraryDependencies ++= Seq(
  "com.itv" %% "scalapact-argonaut-6-2"  % "2.2.5",
  "com.itv" %% "scalapact-http4s-0-16-2" % "2.2.5"
)

addSbtPlugin("com.itv" % "sbt-scalapact" % "2.2.5")
```
In you're test suite, you will need the following import for Consumer tests:
```scala
  import com.itv.scalapact.ScalaPactForger._
```
Or this one for Verification tests:
```scala
  import com.itv.scalapact.ScalaPactVerify._
``` 

Note that you can use different versions of Scala-Pact with the plugin and the testing framework, which can make Scala 2.10 compat issues easier to work around while we get the SBT 1.0 release sorted out.
