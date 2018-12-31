# Scala-Pact

A Consumer Driven Contract testing library for Scala and [ScalaTest](http://www.scalatest.org/) that follows the [Pact](https://docs.pact.io/) standard.

Scala-Pact is intended for Scala developers who are looking for a better way to manage the HTTP contracts between their services.

## Latest version is 2.3.4

Scala-Pact now has two branches based on SBT requirements.

#### SBT 1.x compatible (Latest 2.3.4)

The all development going forward begins at `2.3.x` and resides on the `master` branch.
For the sake of the maintainer's sanity, version 2.3.x and beyond will only support Scala 2.12 and SBT 1.x or greater.

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

Add the following lines to your `build.sbt` file to setup the test framework:
```scala

import com.itv.scalapact.plugin._

enablePlugins(ScalaPactPlugin)
        
libraryDependencies ++= Seq(
  "com.itv"       %% "scalapact-circe-0-9"     % "2.3.4" % "test",
  "com.itv"       %% "scalapact-http4s-0-18"   % "2.3.4" % "test",
  "com.itv"       %% "scalapact-scalatest"     % "2.3.4" % "test",
  "org.scalatest" %% "scalatest"               % "3.0.5" % "test"
)
```

Add this line to your `project/plugins.sbt` file to install the plugin:
```scala
addSbtPlugin("com.itv" % "sbt-scalapact" % "2.3.4")
```
This version of the plugin comes pre-packaged with the latest JSON and Http libraries.
Thanks to the way SBT works, that one plugin line will work in most cases, but if you're still having conflicts, you can also do this to use your preferred libraries:

```scala
 libraryDependencies ++= Seq(
   "com.itv" %% "scalapact-argonaut-6-2" % "2.3.4",
   "com.itv" %% "scalapact-http4s-0-16a" % "2.3.4"
 )
 
 addSbtPlugin("com.itv" % "sbt-scalapact-nodeps" % "2.3.4")
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
Alternatively, these are also valid if you prefer:
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
