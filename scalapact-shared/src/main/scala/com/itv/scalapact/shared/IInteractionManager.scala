package com.itv.scalapact.shared

trait IInteractionManager {

  def findMatchingInteraction(request: InteractionRequest, strictMatching: Boolean): Either[String, Interaction]

  def getInteractions: List[Interaction]

  def addInteraction(interaction: Interaction): Unit

  def addInteractions(interactions: List[Interaction]): Unit

  def clearInteractions(): Unit

  def addToInteractionManager: ConfigAndPacts => ScalaPactSettings

}
