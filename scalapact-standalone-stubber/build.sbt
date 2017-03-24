name := "scalapact-standalone-stubber"

organization := "com.itv"

version := "2.1.4-SNAPSHOT"

scalaVersion := "2.12.1"

libraryDependencies <++= version { scalapactVersion =>
  Seq(
    "com.itv" %% "scalapact-core" % scalapactVersion
  )
}
