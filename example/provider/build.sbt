organization := "com.example"

name := "provider"

version := "0.0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.21.7",
  "org.http4s" %% "http4s-dsl"          % "0.21.7",
  "org.http4s" %% "http4s-circe"        % "0.21.7",
  "org.slf4j"  % "slf4j-simple"         % "1.6.4",
)
