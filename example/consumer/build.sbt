name := "consumer"

organization := "com.example"

version := "0.0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalaj"     %% "scalaj-http"         % "1.1.5",
  "org.slf4j"      % "slf4j-simple"         % "1.6.4",
  "org.json4s"     %% "json4s-native"       % "3.3.0",
  "org.scalatest"  %% "scalatest"           % "2.2.1" % "test",
  "com.itv"        %% "scalapact-scalatest" % "2.0.0-RC2" % "test"
)

initialCommands := "import com.example.consumer._"
