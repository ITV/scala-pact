import java.io.File
import com.itv.scalapact.plugin._

name := "provider"

scalaVersion := "2.13.3"

enablePlugins(ScalaPactPlugin)

lazy val pactVersionFile: SettingKey[File] = settingKey[File]("location of scala-pact version for these tests")
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
    "com.itv"       %% "scalapact-scalatest-suite" % pactVersion % "test",
    "org.scalaj"    %% "scalaj-http"               % "2.4.2" % "test",
    "io.circe"      %% "circe-parser"              % "0.13.0"
  )
}
