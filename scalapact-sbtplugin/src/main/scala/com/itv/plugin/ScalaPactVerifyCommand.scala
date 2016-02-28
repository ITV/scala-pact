package com.itv.plugin

import sbt._

object ScalaPactVerifyCommand {
  lazy val pactVerifyCommandHyphen: Command = Command.command("pact-verify")(pactVerify)
  lazy val pactVerifyCommandCamel: Command = Command.command("pactVerify")(pactVerify)

  private lazy val pactVerify: State => State = state => {

    println("Placeholder for ScalaPact verification command")

    state
  }
}
