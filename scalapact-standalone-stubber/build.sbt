name := "scalapact-standalone-stubber"

organization := "com.itv"

version := "2.0.0-RC1"

scalaVersion := "2.11.7"

libraryDependencies <++= version { scalapactVersion =>
  Seq(
    "org.scalatest" %% "scalatest" % "2.2.1" % "test" withSources() withJavadoc(),
    "com.itv" %% "scalapact-core" % scalapactVersion
  )
}
