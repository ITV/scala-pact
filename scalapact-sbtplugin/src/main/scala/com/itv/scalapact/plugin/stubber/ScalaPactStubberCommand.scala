package com.itv.scalapact.plugin.stubber

import sbt._

object ScalaPactStubberCommand {

  lazy val pactStubberCommandHyphen: Command = Command.args("pact-stubber", "<options>")(pactStubber)
  lazy val pactStubberCommandCamel: Command = Command.args("pactStubber", "<options>")(pactStubber)

  private lazy val pactStubber: (State, Seq[String]) => State = (state, args) => {
    val pactTestedState = Command.process("pact-test", state)

    (StubArguments.parseArguments andThen LocalPactFileLoader.loadPactFiles andThen PactStubService.startServer)(args)

    pactTestedState
  }

}