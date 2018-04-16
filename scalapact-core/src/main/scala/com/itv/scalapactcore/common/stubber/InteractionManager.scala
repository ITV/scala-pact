package com.itv.scalapactcore.common.stubber

import com.itv.scalapact.shared._
import ColourOuput._
import com.itv.scalapact.shared.PactLogger
import com.itv.scalapact.shared.typeclasses.IPactReader

class InteractionManager extends IInteractionManager {

  import com.itv.scalapactcore.common.matching.InteractionMatchers._

  private var interactions = List.empty[Interaction]

  def findMatchingInteraction(request: InteractionRequest,
                              strictMatching: Boolean)(implicit pactReader: IPactReader): Either[String, Interaction] =
    matchRequest(strictMatching, interactions.map(_.withoutSslHeader), request)

  def getInteractions: List[Interaction] = interactions

  def addInteraction(interaction: Interaction): Unit = interactions = interaction :: interactions

  def addInteractions(interactions: List[Interaction]): Unit = interactions.foreach(addInteraction)

  def clearInteractions(): Unit = interactions = List.empty[Interaction]

  def addToInteractionManager: ConfigAndPacts => ScalaPactSettings = configAndPacts => {
    configAndPacts.pacts.foreach { p =>
      PactLogger.debug(("> Adding interactions:\n> - " + p.interactions.mkString("\n> - ")).blue)
      addInteractions(p.interactions)
    }

    configAndPacts.scalaPactSettings
  }

}
