name := "scalapact-standalone-stubber"

organization := "com.itv"

version := "2.0.0-RC2"

scalaVersion := "2.11.7"

libraryDependencies <++= version { scalapactVersion =>
  Seq(
    "com.itv" %% "scalapact-core" % scalapactVersion
  )
}
