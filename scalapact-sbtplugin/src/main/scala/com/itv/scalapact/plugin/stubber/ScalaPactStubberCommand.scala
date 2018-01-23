package com.itv.scalapact.plugin.stubber

import com.itv.scalapact.plugin.ScalaPactPlugin
import sbt._
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapactcore.stubber.InteractionManager
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.{ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapact.shared.PactLogger

object ScalaPactStubberCommand {

//  lazy val pactStubberCommandHyphen: Command = Command.args("pact-stubber", "<options>")(pactStubber)
//  lazy val pactStubberCommandCamel: Command = Command.args("pactStubber", "<options>")(pactStubber)
//
//  private lazy val pactStubber: (State, Seq[String]) => State = (state, args) => {
//
//    PactLogger.message("*************************************".white.bold)
//    PactLogger.message("** ScalaPact: Running Stubber      **".white.bold)
//    PactLogger.message("*************************************".white.bold)
//
//    val pactTestedState = Command.process("pact-test", state)
//
//    runStubber(
//      Project.extract(state).get(ScalaPactPlugin.autoImport.scalaPactEnv).toSettings + ScalaPactSettings.parseArguments(args),
//      interactionManagerInstance
//    )
//
//    pactTestedState
//  }


  def interactionManagerInstance: InteractionManager = new InteractionManager

  def runStubber(scalaPactSettings: ScalaPactSettings, interactionManager: InteractionManager): Unit = {
    (loadPactFiles(pactReader)(true)(scalaPactSettings.giveOutputPath) andThen interactionManager.addToInteractionManager andThen startServer(interactionManager, sslContextName = None)(pactReader, pactWriter, implicitly[SslContextMap]))(scalaPactSettings)
  }

}