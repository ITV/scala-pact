val compilerOptions212 = scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xfuture", // Turn on future language features.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match", // Pattern match may not be typesafe.
  "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ypartial-unification", // Enable partial unification in type constructor inference
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
)

addCommandAlias(
  "quickcompile",
  ";shared/compile;core/compile;argonaut62/compile;http4s016a/compile;pactSpec/compile;pluginShared/compile;plugin/compile;pluginNoDeps/compile;standalone/compile;framework/compile;testsWithDeps/compile"
)
addCommandAlias(
  "quicktest",
  ";shared/test;core/test;argonaut62/test;http4s016a/test;pactSpec/test;pluginShared/test;plugin/test;pluginNoDeps/test;standalone/test;framework/test;testsWithDeps/test"
)
addCommandAlias(
  "quickpublish",
  ";shared/publishLocal;core/publishLocal;argonaut62/publishLocal;circe08/publishLocal;circe09/publishLocal;http4s016a/publishLocal;http4s017/publishLocal;http4s018/publishLocal;pluginShared/publishLocal;plugin/publishLocal;pluginNoDeps/publishLocal;standalone/publishLocal;framework/publishLocal"
)

lazy val commonSettings = Seq(
  version := "2.3.0-SNAPSHOT",
  organization := "com.itv",
  scalaVersion := scala212,
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
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
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
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

val scala212: String = "2.12.5"

lazy val shared =
  (project in file("scalapact-shared"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-shared",
      libraryDependencies ++= Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.6")
    )
    .settings(compilerOptions212: _*)

lazy val core =
  (project in file("scalapact-core"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-core"
    )
    .dependsOn(shared)
    .settings(compilerOptions212: _*)

lazy val http4s016a =
  (project in file("scalapact-http4s-0-16a"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-http4s-0-16a",
      libraryDependencies ++= Seq(
        "org.http4s"             %% "http4s-blaze-server" % "0.16.6a",
        "org.http4s"             %% "http4s-blaze-client" % "0.16.6a",
        "org.http4s"             %% "http4s-dsl"          % "0.16.6a",
        "com.github.tomakehurst" % "wiremock"             % "1.56" % "test"
      )
    )
    .dependsOn(shared)
    .settings(compilerOptions212: _*)

lazy val http4s017 =
  (project in file("scalapact-http4s-0-17"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(mockSettings: _*)
    .settings(
      name := "scalapact-http4s-0-17",
      libraryDependencies ++= Seq(
        "org.http4s"             %% "http4s-blaze-server" % "0.17.6",
        "org.http4s"             %% "http4s-blaze-client" % "0.17.6",
        "org.http4s"             %% "http4s-dsl"          % "0.17.6",
        "com.github.tomakehurst" % "wiremock"             % "1.56" % "test"
      )
    )
    .dependsOn(shared)
    .settings(compilerOptions212: _*)

lazy val http4s018 =
  (project in file("scalapact-http4s-0-18"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-http4s-0-18",
      libraryDependencies ++= Seq(
        "org.http4s"             %% "http4s-blaze-server" % "0.18.9",
        "org.http4s"             %% "http4s-blaze-client" % "0.18.9",
        "org.http4s"             %% "http4s-dsl"          % "0.18.9",
        "com.github.tomakehurst" % "wiremock"             % "1.56" % "test"
      )
    )
    .dependsOn(shared)
    .settings(compilerOptions212: _*)

lazy val argonaut62 =
  (project in file("scalapact-argonaut-6-2"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-argonaut-6-2",
      libraryDependencies ++= Seq(
        "io.argonaut" %% "argonaut" % "6.2"
      )
    )
    .dependsOn(shared)
    .settings(compilerOptions212: _*)

lazy val circe08 =
  (project in file("scalapact-circe-0-8"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-circe-0-8",
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-core",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-parser"
      ).map(_ % "0.8.0")
    )
    .dependsOn(shared)
    .settings(compilerOptions212: _*)

lazy val circe09 =
  (project in file("scalapact-circe-0-9"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-circe-0-9",
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-core",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-parser"
      ).map(_ % "0.9.3")
    )
    .dependsOn(shared)
    .settings(compilerOptions212: _*)

lazy val pluginShared =
  (project in file("sbt-scalapact-shared"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "sbt-scalapact-shared",
      scalaVersion := scala212
    )
    .dependsOn(core)
    .settings(compilerOptions212: _*)

lazy val plugin =
  (project in file("sbt-scalapact"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "sbt-scalapact",
      sbtPlugin := true,
      scalaVersion := scala212
    )
    .dependsOn(pluginShared)
    .dependsOn(argonaut62)
    .dependsOn(http4s018)
    .settings(compilerOptions212: _*)

lazy val pluginNoDeps =
  (project in file("sbt-scalapact-nodeps"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "sbt-scalapact-nodeps",
      sbtPlugin := true,
      scalaVersion := scala212
    )
    .dependsOn(pluginShared)
    .dependsOn(argonaut62 % "provided")
    .dependsOn(http4s018 % "provided")
    .settings(compilerOptions212: _*)

lazy val framework =
  (project in file("scalapact-scalatest"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(
      name := "scalapact-scalatest",
      mappings in (Compile, packageBin) ~= {
        _.filterNot { case (_, fileName) => fileName == "logback.xml" || fileName == "log4j.properties" }
      }
    )
    .dependsOn(core)
    .settings(compilerOptions212: _*)

lazy val standalone =
  (project in file("scalapact-standalone-stubber"))
    .settings(commonSettings: _*)
    .settings(
      name := "scalapact-standalone-stubber",
      scalaVersion := scala212
    )
    .dependsOn(core)
    .dependsOn(circe09)
    .dependsOn(http4s018)
    .settings(compilerOptions212: _*)

lazy val pactSpec =
  (project in file("pact-spec-tests"))
    .settings(commonSettings: _*)
    .settings(
      name := "pact-spec-tests",
      scalaVersion := scala212
    )
    .dependsOn(core)
    .dependsOn(argonaut62)

lazy val testsWithDeps =
  (project in file("tests-with-deps"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scalaj"             %% "scalaj-http"   % "2.3.0" % "test",
        "org.json4s"             %% "json4s-native" % "3.5.0" % "test",
        "com.github.tomakehurst" % "wiremock"       % "1.56"  % "test",
        "fr.hmil"                %% "roshttp"       % "2.0.1" % "test"
      )
    )
    .dependsOn(framework)
    .dependsOn(argonaut62)
    .dependsOn(http4s017)

lazy val docs =
  (project in file("scalapact-docs"))
    .settings(commonSettings: _*)
    .enablePlugins(ParadoxPlugin)
    .enablePlugins(ParadoxSitePlugin)
    .enablePlugins(GhpagesPlugin)
    .settings(
      scalaVersion := scala212,
      paradoxTheme := Some(builtinParadoxTheme("generic")),
      name := "scalapact-docs",
      git.remoteRepo := "git@github.com:ITV/scala-pact.git",
      sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox"
    )

lazy val scalaPactProject =
  (project in file("."))
    .settings(commonSettings: _*)
    .aggregate(shared, core, pluginShared, plugin, pluginNoDeps, framework, standalone)
    .aggregate(http4s016a, http4s017, http4s018)
    .aggregate(argonaut62, circe08, circe09)
    .aggregate(docs)
    .aggregate(pactSpec, testsWithDeps)
