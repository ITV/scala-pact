package com.itv.scalapact.model

import scala.util.Properties

final case class ScalaPactOptions(writePactFiles: Boolean, outputPath: String)

object ScalaPactOptions {
  val DefaultOptions: ScalaPactOptions =
    ScalaPactOptions(writePactFiles = true, outputPath = Properties.envOrElse("pact.rootDir", "target/pacts"))
}
