package com.itv.plugin

import sbt._

object ScalaPactStubberCommand {
  lazy val pactStubberCommandHyphen: Command = Command.command("pact-stubber")(pactStubber)
  lazy val pactStubberCommandCamel: Command = Command.command("pactStubber")(pactStubber)

  private lazy val pactStubber: State => State = state => {

    println("Placeholder for ScalaPact stubber command")

    state
  }
}
