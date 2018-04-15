package com.itv.scalapact.shared

import com.itv.scalapact.shared.typeclasses.IPactReader

trait IInteractionManager {

  def findMatchingInteraction(request: InteractionRequest, strictMatching: Boolean)(
      implicit pactReader: IPactReader): Either[String, Interaction]

  def getInteractions: List[Interaction]

  def addInteraction(interaction: Interaction): Unit

  def addInteractions(interactions: List[Interaction]): Unit

  def clearInteractions(): Unit

  def addToInteractionManager: ConfigAndPacts => ScalaPactSettings

}
