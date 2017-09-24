package com.itv.scalapact.shared.pact

object EitherWithToOption {

  import scala.language.implicitConversions

  case class WithToOption[A, B](either: Either[A, B]) {
    def toOption: Option[B] =
      either match {
        case Right(r) =>
          Some(r)

        case Left(_) =>
          None
      }
  }

  implicit def toWithToOption[A, B](either: Either[A, B]): WithToOption[A, B] =
    WithToOption(either)
}
