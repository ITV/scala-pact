package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.MessageContentType

case class MessageFormatError(msg: String) extends Throwable

trait IMessageFormat[T] {
  def contentType: MessageContentType
  def encode(t: T): String
  def decode(s: String): Either[MessageFormatError, T]
}
