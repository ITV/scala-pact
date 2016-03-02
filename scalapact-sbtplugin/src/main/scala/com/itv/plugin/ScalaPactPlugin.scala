package com.itv.plugin

import com.itv.plugin.stubber.ScalaPactStubberCommand
import com.itv.plugin.tester.ScalaPactTestCommand
import com.itv.plugin.verifier.ScalaPactVerifyCommand
import sbt.Keys._
import sbt._

import scala.language.implicitConversions

object ScalaPactPlugin extends Plugin {
  override lazy val settings = Seq(
    commands += ScalaPactTestCommand.pactTestCommandHyphen,
    commands += ScalaPactTestCommand.pactTestCommandCamel,
    commands += ScalaPactVerifyCommand.pactVerifyCommandHyphen,
    commands += ScalaPactVerifyCommand.pactVerifyCommandCamel,
    commands += ScalaPactStubberCommand.pactStubberCommandHyphen,
    commands += ScalaPactStubberCommand.pactStubberCommandCamel
  )
}
