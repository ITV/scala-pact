package com.itv.scalapact.shared

import com.itv.scalapact.shared.json.IPactReader

trait IInteractionManager {

  def findMatchingInteraction(request: InteractionRequest)(implicit
      pactReader: IPactReader
  ): Either[String, Interaction]

  def getInteractions: List[Interaction]

  def addInteraction(interaction: InteractionWithStrictness): Unit

  def addInteractions(interactions: List[InteractionWithStrictness]): Unit

  def clearInteractions(): Unit

  def addToInteractionManager: List[(Pact, Boolean)] => Unit

}
