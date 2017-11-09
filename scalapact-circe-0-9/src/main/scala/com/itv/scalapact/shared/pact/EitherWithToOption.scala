package com.itv.scalapact.shared.pact

object EitherWithToOption {

  implicit class WithToOption[A, B](either: Either[A, B]) {
    def toOption: Option[B] =
      either match {
        case Right(r) =>
          Some(r)

        case Left(_) =>
          None
      }
  }
}
