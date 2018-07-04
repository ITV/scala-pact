package com.itv.scalapactcore.message

import com.itv.scalapact.shared.Message
import com.itv.scalapact.shared.typeclasses.IMessageFormat
import com.itv.scalapactcore.common.matching.MatchOutcome

trait IMessageStubber[A] {
  def consume(description: String)(test: Message => A): IMessageStubber[A]
  def publish[T](description: String, actualContent: T, meta: Message.Metadata = Message.Metadata.empty)(
      implicit messageFormat: IMessageFormat[T]
  ): IMessageStubber[A]
  def results: List[A]
  def outcome: MatchOutcome
}
