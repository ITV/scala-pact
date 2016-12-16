name := "scalapact-standalone-stubber"

organization := "com.itv"

version := "2.0.1-SNAPSHOT"

scalaVersion := "2.12.1"

libraryDependencies <++= version { scalapactVersion =>
  Seq(
    "com.itv" %% "scalapact-core" % scalapactVersion
  )
}
