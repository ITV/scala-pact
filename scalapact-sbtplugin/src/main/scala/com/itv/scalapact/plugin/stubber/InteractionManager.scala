package com.itv.scalapact.plugin.stubber

import com.itv.scalapact.plugin.common.Rainbow._
import com.itv.scalapact.plugin.common.{Arguments, ConfigAndPacts, InteractionMatchers}
import com.itv.scalapactcore.{Interaction, InteractionRequest}

import scalaz.\/

object InteractionManager extends InteractionManager

//Use trait for testing or you'll have race conditions!
trait InteractionManager {

  import InteractionMatchers._

  private var interactions = List.empty[Interaction]

  def findMatchingInteraction(request: InteractionRequest): \/[String, Interaction] =
    matchRequest(interactions)(request)

  def getInteractions: List[Interaction] = interactions

  def addInteraction(interaction: Interaction): Unit = interactions = interaction :: interactions

  def addInteractions(interactions: List[Interaction]): Unit = interactions.foreach(addInteraction)

  def clearInteractions(): Unit = interactions = List.empty[Interaction]

  lazy val addToInteractionManager: ConfigAndPacts => Arguments = configAndPacts => {
    configAndPacts.pacts.foreach { p =>
      println(("> Adding interactions:\n> - " + p.interactions.mkString("\n> - ")).blue)
      addInteractions(p.interactions)
    }

    configAndPacts.arguments
  }

}
