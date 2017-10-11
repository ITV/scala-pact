package com.itv.standalonestubber

import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.ScalaPactSettings
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.stubber.InteractionManager
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapactcore.common.PactReaderWriter._

object PactStubber {

  def main(args: Array[String]): Unit = {

    println("*************************************".white.bold)
    println("** ScalaPact: Running Stubber      **".white.bold)
    println("*************************************".white.bold)

    val interactionManager: InteractionManager = new InteractionManager

    (ScalaPactSettings.parseArguments andThen loadPactFiles(pactReader)(true)("pacts") andThen interactionManager.addToInteractionManager andThen startServer(interactionManager)(pactReader, pactWriter))(args)

  }

}

