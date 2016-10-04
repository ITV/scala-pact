name := "scalapact-standalone-stubber"

organization := "com.itv"

version := "1.0.2"

scalaVersion := "2.11.7"

libraryDependencies <++= version { scalapactVersion =>
  Seq(
    "org.scalatest" %% "scalatest" % "2.2.1" % "test" withSources() withJavadoc(),
    "com.itv" %% "scalapact-core" % scalapactVersion
  )
}
