name := "consumer"

organization := "com.example"

version := "0.0.1"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "org.scalaj"     %% "scalaj-http"         % "2.3.0",
  "org.slf4j"      % "slf4j-simple"         % "1.6.4",
  "org.json4s"     %% "json4s-native"       % "3.5.0",
  "org.scalatest"  %% "scalatest"           % "3.0.0" % "test",
  "com.itv"        %% "scalapact-scalatest" % "2.1.0" % "test"
)

initialCommands := "import com.example.consumer._"
