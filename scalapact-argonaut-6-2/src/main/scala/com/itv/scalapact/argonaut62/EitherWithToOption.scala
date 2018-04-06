package com.itv.scalapact.argonaut62

object EitherWithToOption {
  implicit class WithAsOption[A, B](either: Either[A, B]) {
    def asOption: Option[B] =
      either match {
        case Right(r) =>
          Some(r)

        case Left(_) =>
          None
      }
  }
}
