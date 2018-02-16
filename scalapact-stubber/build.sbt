name := "scalapact-stubber"
lazy val http4sVersion = "0.16.6a"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "com.github.tomakehurst" % "wiremock" % "1.56" % "test"
)

