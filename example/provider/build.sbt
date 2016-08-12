organization := "com.example"
name := "provider"
version := "0.0.1"
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.14.2a",
  "org.http4s" %% "http4s-dsl"          % "0.14.2a",
  "org.http4s" %% "http4s-argonaut"     % "0.14.2a"
)

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"
