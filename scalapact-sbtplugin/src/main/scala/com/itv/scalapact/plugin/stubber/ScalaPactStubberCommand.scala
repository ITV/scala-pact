package com.itv.scalapact.plugin.stubber

import java.io.File
import java.util.ResourceBundle
import java.util.concurrent.{ExecutorService, Executors}

import com.ing.pact.stubber.ConfigBasedStubber
import com.ing.pact.stubber.Stubber.args
import com.itv.scalapact.plugin.ScalaPactPlugin
import sbt._
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapactcore.stubber.InteractionManager
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.{Helpers, PactLogger, ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.common.PactReaderWriter._

import scala.util.Try

object ScalaPactStubberCommand {

  lazy val pactStubberCommandHyphen: Command = Command.args("pact-stubber", "<options>")(pactStubber)
  lazy val pactStubberCommandCamel: Command = Command.args("pactStubber", "<options>")(pactStubber)

  private lazy val pactStubber: (State, Seq[String]) => State = (state, args) => {

    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Running Stubber      **".white.bold)
    PactLogger.message("*************************************".white.bold)


    implicit class MapPimper(map: Map[String, String]) {
      def getBoolean(key: String): Boolean = map.get(key).map(_.toLowerCase) match {
        case Some("true") => true
        case Some("false") => false
        case None => throw new IllegalArgumentException(s"Tried to get $key out of map, but only keys are [${map.keySet.toList.sorted.mkString(",")}]")
        case x => throw new IllegalArgumentException(s"Argument for $key is $x but should be 'true' or 'false'")
      }
    }

    val argMap = Map("--runTests" -> "false") ++ (Helpers.pair apply args.toList)

    val pactTestedState: State = if (argMap.getBoolean("--runTests")) Command.process("pact-test", state) else state
    argMap get "--file" match {
      case Some(fileName) =>
        implicit val resources: ResourceBundle = ResourceBundle.getBundle("messages")
        implicit val executorService: ExecutorService = Executors.newFixedThreadPool(10)
        ConfigBasedStubber(new File(fileName)).waitForever()
      case None =>
        runStubber(
          Project.extract(state).get(ScalaPactPlugin.autoImport.scalaPactEnv).toSettings + ScalaPactSettings.convertToArguments(argMap),
          interactionManagerInstance
        )
    }

    pactTestedState
  }

  def interactionManagerInstance: InteractionManager = new InteractionManager

  def runStubber(scalaPactSettings: ScalaPactSettings, interactionManager: InteractionManager): Unit = {
    (loadPactFiles(pactReader)(true)(scalaPactSettings.giveOutputPath) andThen interactionManager.addToInteractionManager andThen startServer(interactionManager, optContextNameAndClientAuth = None)(pactReader, pactWriter, implicitly[SslContextMap])) (scalaPactSettings)
  }

}