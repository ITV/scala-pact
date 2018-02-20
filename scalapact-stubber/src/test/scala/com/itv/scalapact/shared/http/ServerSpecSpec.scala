package com.itv.scalapact.shared.http

import com.ing.pact.stubber.ServerSpec
import com.itv.scalapact.shared._
import com.itv.scalapactcore.stubber.InteractionManager
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class ServerSpecSpec extends FlatSpec with Matchers with MockitoSugar {

  behavior of "ServerSpec.interactionManager"

  val int11 = Interaction(None, None, "interaction11", mock[InteractionRequest], mock[InteractionResponse])
  val int12 = Interaction(None, None, "interaction12", mock[InteractionRequest], mock[InteractionResponse])
  val int21 = Interaction(None, None, "interaction21", mock[InteractionRequest], mock[InteractionResponse])
  val int22 = Interaction(None, None, "interaction21", mock[InteractionRequest], mock[InteractionResponse])
  val pact1 = Pact(PactActor("provider1"), mock[PactActor], List(int11, int12))
  val pact2 = Pact(PactActor("provider2"), mock[PactActor], List(int21, int22))

  it should "allow an interaction manager to be build with the pacts that have the specified provider" in {

    val interactionManager = mock[InteractionManager]
    val result = ServerSpec.interactionManager(Seq(pact1, pact2), Some("provider1"), interactionManager)
    result shouldBe interactionManager

    verify(interactionManager, times(1)).addInteractions(List(int11, int12))
    verify(interactionManager, times(1)).addInteractions(any[List[Interaction]])

    verify(interactionManager, times(0)).addInteraction(any[Interaction])
  }

  it should "allow the interaction manager to be built with all pacts if not specified" in {
    val interactionManager = mock[InteractionManager]
    val result = ServerSpec.interactionManager(Seq(pact1, pact2), None, interactionManager)
    result shouldBe interactionManager

    verify(interactionManager, times(1)).addInteractions(List(int11, int12))
    verify(interactionManager, times(1)).addInteractions(List(int21, int22))
    verify(interactionManager, times(2)).addInteractions(any[List[Interaction]])

    verify(interactionManager, times(0)).addInteraction(any[Interaction])

  }

}
