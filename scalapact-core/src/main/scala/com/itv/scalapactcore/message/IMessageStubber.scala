package com.itv.scalapactcore.message

import com.itv.scalapact.shared.Message
import com.itv.scalapact.shared.typeclasses.IMessageFormat

trait IMessageStubber[A] {
  def consume(description: String)(test: Message => A): IMessageStubber[A]
  def publish[T](description: String, actualContent: T, meta: Message.Metadata = Message.Metadata.empty)(
      implicit messageFormat: IMessageFormat[T]
  ): IMessageStubber[A]
  def currentResult: List[Either[String, A]]
}
