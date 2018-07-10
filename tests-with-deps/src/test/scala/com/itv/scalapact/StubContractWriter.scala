package com.itv.scalapact

import java.util.concurrent.atomic.AtomicReference

import com.itv.scalapact.ScalaPactForger.messageSpec.IContractWriter
import com.itv.scalapact.shared.Message

case class StubContractWriter(
    actualPact: AtomicReference[Option[ScalaPactForger.ScalaPactDescriptionFinal]] = new AtomicReference(None)
) extends IContractWriter {

  def messages: List[Message] = actualPact.get().map(_.messages).toList.flatten

  def interactions = actualPact.get().map(_.interactions).toList.flatten

  override def writeContract(scalaPactDescriptionFinal: ScalaPactForger.ScalaPactDescriptionFinal): Unit =
    actualPact.set(Some(scalaPactDescriptionFinal))
}
