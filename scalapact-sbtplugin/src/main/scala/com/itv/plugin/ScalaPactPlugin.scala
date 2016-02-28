package com.itv.plugin

import sbt.Keys._
import sbt._
import scala.language.implicitConversions

object ScalaPactPlugin extends Plugin {
  override lazy val settings = Seq(
    commands += ScalaPactTestCommand.pactCommandHyphen,
    commands += ScalaPactTestCommand.pactCommandCamel
  )
}
