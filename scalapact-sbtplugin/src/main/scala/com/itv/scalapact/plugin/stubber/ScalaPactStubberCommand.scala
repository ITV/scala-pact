package com.itv.scalapact.plugin.stubber

import sbt._

import com.itv.scalapactcore.common.CommandArguments._
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.stubber.PactStubService._
import com.itv.scalapactcore.stubber.InteractionManager
import com.itv.scalapactcore.common.ColourOuput._

object ScalaPactStubberCommand {

  lazy val pactStubberCommandHyphen: Command = Command.args("pact-stubber", "<options>")(pactStubber)
  lazy val pactStubberCommandCamel: Command = Command.args("pactStubber", "<options>")(pactStubber)

  private lazy val pactStubber: (State, Seq[String]) => State = (state, args) => {

    println("*************************************".white.bold)
    println("** ScalaPact: Running Stubber      **".white.bold)
    println("*************************************".white.bold)

    val pactTestedState = Command.process("pact-test", state)

    val interactionManager: InteractionManager = new InteractionManager

    (parseArguments andThen loadPactFiles("target/pacts") andThen interactionManager.addToInteractionManager andThen startServer(interactionManager))(args)

    pactTestedState
  }

}