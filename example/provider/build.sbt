organization := "com.example"

name := "provider"

version := "0.0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.17.6",
  "org.http4s" %% "http4s-dsl"          % "0.17.6",
  "org.http4s" %% "http4s-argonaut"     % "0.17.6",
  "org.slf4j"  % "slf4j-simple"         % "1.6.4"
)
