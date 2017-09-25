package com.itv.standalonestubber

import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapactcore.common.CommandArguments._
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.stubber.InteractionManager
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapactcore.common.PactReaderWriter._

object PactStubber {

  def main(args: Array[String]) {

    println("*************************************".white.bold)
    println("** ScalaPact: Running Stubber      **".white.bold)
    println("*************************************".white.bold)

    val interactionManager: InteractionManager = new InteractionManager

    (parseArguments andThen loadPactFiles(pactReader)("pacts") andThen interactionManager.addToInteractionManager andThen startServer(interactionManager)(pactReader, pactWriter))(args)

  }

}

