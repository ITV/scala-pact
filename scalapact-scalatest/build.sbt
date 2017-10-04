
name := "scalapact-scalatest"

libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0" % "test",
  "org.json4s" %% "json4s-native" % "3.5.0" % "test",
  "com.github.tomakehurst" % "wiremock" % "1.56" % "test",
  "fr.hmil" %% "roshttp" % "2.0.1" % "test"
)
