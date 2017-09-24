organization := "com.example"

name := "provider_tests"

version := "0.0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.http4s"     %% "http4s-blaze-server"    % "0.15.0a",
  "org.http4s"     %% "http4s-dsl"             % "0.15.0a",
  "org.http4s"     %% "http4s-argonaut"        % "0.15.0a",
  "org.slf4j"      %  "slf4j-simple"           % "1.6.4",
  "org.scalatest"  %% "scalatest"              % "3.0.1"          % "test",
  "com.itv"        %% "scalapact-argonaut-6-2" % "2.2.0-SNAPSHOT" % "test",
  "com.itv"        %% "scalapact-scalatest"    % "2.2.0-SNAPSHOT" % "test"
)
