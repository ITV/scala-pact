package com.itv.scalapactcore.common.stubber

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.utils.ColourOutput._
import com.itv.scalapact.shared.json.IPactReader
import com.itv.scalapact.shared.utils.PactLogger

class InteractionManager extends IInteractionManager {

  import com.itv.scalapactcore.common.matching.InteractionMatchers._

  private var interactions = List.empty[InteractionWithStrictness]

  def findMatchingInteraction(request: InteractionRequest)(implicit
      pactReader: IPactReader
  ): Either[String, Interaction] =
    matchRequest(interactions.map(i => i.copy(interaction = i.interaction.withoutSslHeader)), request)

  def getInteractions: List[Interaction] = interactions.map(_.interaction)

  def addInteraction(interaction: InteractionWithStrictness): Unit = interactions = interaction :: interactions

  def addInteractions(interactions: List[InteractionWithStrictness]): Unit = interactions.foreach(addInteraction)

  def clearInteractions(): Unit = interactions = List.empty[InteractionWithStrictness]

  def addToInteractionManager: List[(Pact, Boolean)] => Unit =
    _.foreach { case (p, strictness) =>
      PactLogger.debug(("> Adding interactions:\n> - " + p.interactions.mkString("\n> - ")).blue)
      addInteractions(p.interactions.map(InteractionWithStrictness(_, strictness)))
    }

}
