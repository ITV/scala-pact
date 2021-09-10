organization := "com.example"

name := "provider"

version := "0.0.1"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.23.1",
  "org.http4s" %% "http4s-dsl"          % "0.23.1",
  "org.http4s" %% "http4s-circe"        % "0.23.1",
  "org.slf4j"   % "slf4j-simple"        % "1.7.32"
)
