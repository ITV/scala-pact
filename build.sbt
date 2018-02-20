
// Pinched shamelessly from https://tpolecat.github.io/2014/04/11/scalac-flags.html
val options210 = Seq(
  //  "-Yno-imports", // Powerful but boring. Essentially you have to pull in everything... one day.
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  //    "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code", // N.B. doesn't work well with the ??? hole
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

addCommandAlias("quickcompile", ";shared_2_12/compile;core_2_12/compile;argonaut62_2_12/compile;pactSpec_2_12/compile;plugin/compile;standalone/compile;framework_2_12/compile")
addCommandAlias("quicktest", ";shared_2_12/test;core_2_12/test;argonaut62_2_12/test;pactSpec_2_12/test;plugin/test;standalone/test;framework_2_12/test")

lazy val commonSettings = Seq(
  version := "2.2.4-SNAPSHOT",
  organization := "com.itv",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  ),
  scalacOptions ++= options210,
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
  )
)

lazy val complexStubberSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.3.1"
  )
)


lazy val mockSettings = Seq(
  // https://mvnrepository.com/artifact/org.mockito/mockito-all
  libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % Test

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

val scala210: String = "2.10.6"
val scala211: String = "2.11.11"
val scala212: String = "2.12.3"

lazy val shared =
  (project in file("scalapact-shared"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val shared_2_10 = shared(scala210)
lazy val shared_2_11 = shared(scala211)
  .settings(
    libraryDependencies ++= Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.6")
  )
lazy val shared_2_12 = shared(scala212)
  .settings(
    libraryDependencies ++= Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.6")
  )

lazy val core =
  (project in file("scalapact-core"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val core_2_10 = core(scala210)
  .dependsOn(shared_2_10)
  .dependsOn(argonaut62_2_10 % "provided")
  .dependsOn(http4s0162a_2_10 % "provided")
  .project
lazy val core_2_11 = core(scala211)
  .dependsOn(shared_2_11)
  .dependsOn(argonaut62_2_11 % "provided")
  .dependsOn(http4s0162a_2_11 % "provided")
  .project
lazy val core_2_12 = core(scala212)
  .dependsOn(shared_2_12)
  .dependsOn(argonaut62_2_12 % "provided")
  .dependsOn(http4s0162a_2_12 % "provided")
  .project

lazy val http4s0150a =
  (project in file("scalapact-http4s-0-15-0a"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val http4s0150a_2_10 = http4s0150a(scala210).dependsOn(shared_2_10)
lazy val http4s0150a_2_11 = http4s0150a(scala211).dependsOn(shared_2_11)
lazy val http4s0150a_2_12 = http4s0150a(scala212).dependsOn(shared_2_12)

lazy val http4s0162a =
  (project in file("scalapact-http4s-0-16-2a"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val http4s0162a_2_10 = http4s0162a(scala210).dependsOn(shared_2_10)
lazy val http4s0162a_2_11 = http4s0162a(scala211).dependsOn(shared_2_11)
lazy val http4s0162a_2_12 = http4s0162a(scala212).dependsOn(shared_2_12)

lazy val http4s0162 =
  (project in file("scalapact-http4s-0-16-2"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val http4s0162_2_10 = http4s0162(scala210).dependsOn(shared_2_10)
lazy val http4s0162_2_11 = http4s0162(scala211).dependsOn(shared_2_11)
lazy val http4s0162_2_12 = http4s0162(scala212).dependsOn(shared_2_12)

lazy val http4s0170 =
  (project in file("scalapact-http4s-0-17-0"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(mockSettings: _*)
    .cross

//lazy val http4s0170_2_10 = http4s0170(scala210).dependsOn(shared_2_10)
lazy val http4s0170_2_11 = http4s0170(scala211).dependsOn(shared_2_11)
lazy val http4s0170_2_12 = http4s0170(scala212).dependsOn(shared_2_12)


lazy val http4s0180 =
  (project in file("scalapact-http4s-0-18-0"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val http4s0180_2_11 = http4s0180(scala211).dependsOn(shared_2_11)
lazy val http4s0180_2_12 = http4s0180(scala212).dependsOn(shared_2_12)

lazy val argonaut62 =
  (project in file("scalapact-argonaut-6-2"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val argonaut62_2_10 = argonaut62(scala210).dependsOn(shared_2_10)
lazy val argonaut62_2_11 = argonaut62(scala211).dependsOn(shared_2_11)
lazy val argonaut62_2_12 = argonaut62(scala212).dependsOn(shared_2_12)

lazy val argonaut61 =
  (project in file("scalapact-argonaut-6-1"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val argonaut61_2_10 = argonaut61(scala210).dependsOn(shared_2_10)
lazy val argonaut61_2_11 = argonaut61(scala211).dependsOn(shared_2_11)
//lazy val argonaut61_2_12 = argonaut61(scala212).dependsOn(shared_2_12) // No such thing

lazy val circe08 =
  (project in file("scalapact-circe-0-8"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val circe08_2_10 = circe08(scala210).dependsOn(shared_2_10).settings(
  addCompilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
  )
)
lazy val circe08_2_11 = circe08(scala211).dependsOn(shared_2_11)
lazy val circe08_2_12 = circe08(scala212).dependsOn(shared_2_12)

lazy val circe09 =
  (project in file("scalapact-circe-0-9"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val circe09_2_11 = circe09(scala211).dependsOn(shared_2_11)
lazy val circe09_2_12 = circe09(scala212).dependsOn(shared_2_12)

lazy val pactSpec =
  (project in file("pact-spec-tests"))
    .settings(commonSettings: _*)
    .cross

lazy val pactSpec_2_10 = pactSpec(scala210).dependsOn(core_2_10, argonaut62_2_10)
lazy val pactSpec_2_11 = pactSpec(scala211).dependsOn(core_2_11, argonaut62_2_11)
lazy val pactSpec_2_12 = pactSpec(scala212).dependsOn(core_2_12, argonaut62_2_12)

lazy val plugin =
  (project in file("scalapact-sbtplugin"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .dependsOn(core_2_10)
    .dependsOn(scalapactStubber_2_10)
    .dependsOn(argonaut62_2_10 % "provided")
    .dependsOn(http4s0162a_2_10 % "provided")
    .project
    .settings(
      sbtPlugin := true,
      scalaVersion := scala210
    )

lazy val framework =
  (project in file("scalapact-scalatest"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .cross

lazy val framework_2_11 =
  framework(scala211)
    .dependsOn(core_2_11)
    .dependsOn(argonaut62_2_11 % "provided")
    .dependsOn(http4s0162a_2_11 % "provided")
    .project

lazy val framework_2_12 =
  framework(scala212)
    .dependsOn(core_2_12)
    .dependsOn(argonaut62_2_12 % "provided")
    .dependsOn(http4s0170_2_12 % "provided")
    .project

lazy val standalone =
  (project in file("scalapact-standalone-stubber"))
    .settings(commonSettings: _*)
    .dependsOn(scalapactStubber_2_10)
    .dependsOn(argonaut62_2_10)
    .dependsOn(http4s0162a_2_10)
    .settings(
      name := "scalapact-standalone-stubber",
      scalaVersion := scala210
    )

lazy val scalapactStubber =
  (project in file("scalapact-stubber"))
    .settings(complexStubberSettings: _*)
    .settings(mockSettings: _*)
    .cross


lazy val scalapactStubber_2_10 =
  scalapactStubber(scala210)
    .dependsOn(core_2_10)
    .dependsOn(argonaut62_2_10)
    .dependsOn(http4s0162a_2_10)
    .project

lazy val scalapactStubber_2_11 =
  scalapactStubber(scala211)
    .dependsOn(core_2_11)
    .dependsOn(argonaut62_2_11)
    .dependsOn(http4s0162a_2_11)
    .project

lazy val scalapactStubber_2_12 =
  scalapactStubber(scala212)
    .dependsOn(core_2_12)
    .dependsOn(argonaut62_2_12)
    .dependsOn(http4s0162a_2_12)
    .project

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
      core_2_10,
      core_2_11,
      core_2_12,
      plugin,
      framework_2_11,
      framework_2_12,
      standalone,
      shared_2_10,
      shared_2_11,
      shared_2_12,
      scalapactStubber_2_10,
      scalapactStubber_2_11,
      docs,
      http4s0150a_2_10,
      http4s0150a_2_11,
      http4s0150a_2_12,
      http4s0162a_2_10,
      http4s0162a_2_11,
      http4s0162a_2_12,
      http4s0162_2_10,
      http4s0162_2_11,
      http4s0162_2_12,
      http4s0170_2_11,
      http4s0170_2_12,
      http4s0180_2_11,
      http4s0180_2_12,
      argonaut61_2_10,
      argonaut61_2_11,
      argonaut62_2_10,
      argonaut62_2_11,
      argonaut62_2_12,
      circe08_2_10,
      circe08_2_11,
      circe08_2_12,
      circe09_2_11,
      circe09_2_12,
      pactSpec_2_10,
      pactSpec_2_11,
      pactSpec_2_12
    )
