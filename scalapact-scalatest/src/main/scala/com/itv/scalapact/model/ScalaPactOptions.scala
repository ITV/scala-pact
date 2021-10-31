package com.itv.scalapact.model

import scala.util.Properties

final case class ScalaPactOptions(writePactFiles: Boolean, outputPath: String, host: String, port: Int)

object ScalaPactOptions {

  val DefaultOptions: ScalaPactOptions =
    ScalaPactOptions(
      writePactFiles = true,
      outputPath = Properties.envOrElse("pact.rootDir", "target/pacts"),
      host = "localhost",
      port = 0 // `0` means "use any available port".
    )

  def apply(writePactFiles: Boolean, outputPath: String): ScalaPactOptions =
    ScalaPactOptions(writePactFiles, outputPath, DefaultOptions.host, DefaultOptions.port)

}
