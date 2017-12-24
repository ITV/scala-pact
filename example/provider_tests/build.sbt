organization := "com.example"

name := "provider_tests"

version := "0.0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.http4s"     %% "http4s-blaze-server"      % "0.17.0",
  "org.http4s"     %% "http4s-dsl"               % "0.17.0",
  "org.http4s"     %% "http4s-argonaut"          % "0.17.0",
  "org.slf4j"      %  "slf4j-simple"             % "1.6.4",
  "org.scalatest"  %% "scalatest"                % "3.0.1"          % "test",
  "com.itv"        %% "scalapact-circe-0-8"      % "2.2.2-SNAPSHOT" % "test",
  "com.itv"        %% "scalapact-http4s-0-17-0"  % "2.2.2-SNAPSHOT" % "test",
  "com.itv"        %% "scalapact-scalatest"      % "2.2.2-SNAPSHOT" % "test"
)
