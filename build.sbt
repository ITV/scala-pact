
// Pinched shamelessly from https://tpolecat.github.io/2014/04/11/scalac-flags.html
val options211 = Seq(
  "-deprecation",
  "-encoding", "UTF-8",       // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

//Unused since we have project < 2.12
val options212 = Seq(
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:implicitConversions",     // Allow definition of implicit functions called views
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
  "-Xfuture",                          // Turn on future language features.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
  "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match",              // Pattern match may not be typesafe.
  "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ypartial-unification",             // Enable partial unification in type constructor inference
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
  "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",              // Warn if a local definition is unused.
  "-Ywarn-unused:params",              // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",            // Warn if a private member is unused.
  "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
)

lazy val compilerOptionsGeneral =
  scalacOptions ++= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, p)) if p >= 12 => options212
      case Some((2, p)) if p >= 11 => options211
      case _ => Nil
    }
  )

addCommandAlias("quickcompile", ";shared/compile;core/compile;argonaut62/compile;http4s016a/compile;pactSpec/compile;plugin/compile;standalone/compile;framework/compile")
addCommandAlias("quicktest", ";shared/test;core/test;argonaut62/test;http4s016a/test;pactSpec/test;plugin/test;standalone/test;framework/test")

lazy val commonSettings = Seq(
  version := "2.3.0-SNAPSHOT",
  organization := "com.itv",
  scalaVersion := scala212,
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  ),
  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(
    Wart.Overloading,
    Wart.FinalCaseClass,
    Wart.ImplicitConversion,
    Wart.Nothing,
    Wart.ImplicitParameter,
    Wart.NonUnitStatements,
    Wart.Throw,
    Wart.Equals,
    Wart.Recursion,
    Wart.LeakingSealed,
    Wart.Null,
    Wart.Var
  ),
  parallelExecution in Test := false
)

lazy val mockSettings = Seq(
  libraryDependencies += "org.scalamock" %% "scalamock" % "4.0.0" % Test
)

lazy val publishSettings = Seq(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra :=
    <url>https://github.com/ITV/scala-pact</url>
      <licenses>
        <license>
          <name>ITV-OSS</name>
          <url>http://itv.com/itv-oss-licence-v1.0</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <developers>
        <developer>
          <id>davesmith00000</id>
          <name>David Smith</name>
          <organization>ITV</organization>
          <organizationUrl>http://www.itv.com</organizationUrl>
        </developer>
      </developers>
)

val scala211: String = "2.11.11"
val scala212: String = "2.12.4"

lazy val shared =
  (project in file("scalapact-shared"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      crossScalaVersions := Seq(scala211, scala212),
      libraryDependencies ++= Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.6")
    )
    .settings(compilerOptionsGeneral: _*)

lazy val core =
  (project in file("scalapact-core"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(shared)
    .dependsOn(argonaut62)
    .dependsOn(http4s016a)
    .settings(compilerOptionsGeneral: _*)

lazy val http4s016a =
  (project in file("scalapact-http4s-0-16a"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(shared)
    .settings(compilerOptionsGeneral: _*)

lazy val http4s016 =
  (project in file("scalapact-http4s-0-16"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(shared)
    .settings(compilerOptionsGeneral: _*)

lazy val http4s017 =
  (project in file("scalapact-http4s-0-17"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(mockSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(shared)
    .settings(compilerOptionsGeneral: _*)

lazy val http4s018 =
  (project in file("scalapact-http4s-0-18"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(shared)
    .settings(compilerOptionsGeneral: _*)

lazy val argonaut62 =
  (project in file("scalapact-argonaut-6-2"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(shared)
    .settings(compilerOptionsGeneral: _*)

lazy val circe08 =
  (project in file("scalapact-circe-0-8"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(shared)
    .settings(compilerOptionsGeneral: _*)

lazy val circe09 =
  (project in file("scalapact-circe-0-9"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(shared)
    .settings(compilerOptionsGeneral: _*)

lazy val pactSpec =
  (project in file("pact-spec-tests"))
    .settings(commonSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(core)
    .dependsOn(argonaut62)

lazy val plugin =
  (project in file("scalapact-sbtplugin"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      sbtPlugin := true,
      scalaVersion := scala212
    )
    .dependsOn(core)
    .dependsOn(argonaut62 % "provided")
    .dependsOn(http4s017 % "provided")
    .settings(compilerOptionsGeneral: _*)

lazy val framework =
  (project in file("scalapact-scalatest"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(crossScalaVersions := Seq(scala211, scala212))
    .dependsOn(core)
    .dependsOn(argonaut62 % "provided")
    .dependsOn(http4s017 % "provided")
    .settings(compilerOptionsGeneral: _*)

lazy val standalone =
  (project in file("scalapact-standalone-stubber"))
    .settings(commonSettings: _*)
    .settings(
      name := "scalapact-standalone-stubber",
      scalaVersion := scala212
    )
    .dependsOn(core)
    .dependsOn(argonaut62 % "provided")
    .dependsOn(http4s017 % "provided")
    .settings(compilerOptionsGeneral: _*)

lazy val docs =
  (project in file("scalapact-docs"))
    .settings(commonSettings: _*)
    .enablePlugins(ParadoxPlugin).
    settings(
      name := "Scala-Pact Docs",
      crossScalaVersions := Seq(scala212),
      paradoxTheme := Some(builtinParadoxTheme("generic"))
    )

lazy val scalaPactProject =
  (project in file("."))
    .settings(commonSettings: _*)
    .aggregate(
      core.project,
      plugin.project,
      framework.project,
      standalone.project,
      shared.project,
      docs.project,
      http4s016a.project,
      http4s016.project,
      http4s017.project,
      http4s018.project,
      argonaut62.project,
      circe08.project,
      circe09.project,
      pactSpec.project
    )
