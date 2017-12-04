package com.itv.scalapactcore.stubber

import com.itv.scalapact.shared._
import ColourOuput._

class InteractionManager extends IInteractionManager {

  import com.itv.scalapactcore.common.matching.InteractionMatchers._

  private var interactions = List.empty[Interaction]

  def findMatchingInteraction(request: InteractionRequest, strictMatching: Boolean): Either[String, Interaction] =
    matchRequest(strictMatching, interactions.map(_.withoutSslHeader), request)

  def getInteractions: List[Interaction] = interactions

  def addInteraction(interaction: Interaction): Unit = interactions = interaction :: interactions

  def addInteractions(interactions: List[Interaction]): Unit = interactions.foreach(addInteraction)

  def clearInteractions(): Unit = interactions = List.empty[Interaction]

  def addToInteractionManager: ConfigAndPacts => ScalaPactSettings = configAndPacts => {
    configAndPacts.pacts.foreach { p =>
      println(("> Adding interactions:\n> - " + p.interactions.mkString("\n> - ")).blue)
      addInteractions(p.interactions)
    }

    configAndPacts.scalaPactSettings
  }

}
