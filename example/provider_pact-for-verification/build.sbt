import java.io.File

organization := "com.example"

name := "provider_pacts-for-verification"

scalaVersion := "2.13.3"

enablePlugins(ScalaPactPlugin)

lazy val pactVersionFile: SettingKey[File] = settingKey[File]("location of scala-pact version for examples")
pactVersionFile := baseDirectory.value.getParentFile.getParentFile / "version.sbt"

libraryDependencies ++= {
    //A hack so we don't have to manually update the scala-pact version for the examples
    lazy val pactVersion = IO.read(pactVersionFile.value).split('"')(1)
    Seq(
    "org.http4s"    %% "http4s-blaze-server"       % "0.21.7",
    "org.http4s"    %% "http4s-dsl"                % "0.21.7",
    "org.http4s"    %% "http4s-circe"              % "0.21.7",
    "org.slf4j"     % "slf4j-simple"               % "1.6.4",
    "org.scalatest" %% "scalatest"                 % "3.0.8" % "test",
    "com.itv"       %% "scalapact-scalatest-suite"   % pactVersion % "test",
    // Optional for auto-derivation of JSON codecs
    "io.circe" %% "circe-generic" % "0.13.0",
    // Optional for string interpolation to JSON model
    "io.circe" %% "circe-literal" % "0.13.0",
  )
}
