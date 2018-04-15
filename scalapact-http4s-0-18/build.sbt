name := "scalapact-http4s-0-18"

lazy val http4sVersion = "0.18.0"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-dsl"          % http4sVersion,
//  "org.log4s"  %% "log4s"               % "1.3.3" force(),
  "com.github.tomakehurst" % "wiremock" % "1.56" % "test"
)
