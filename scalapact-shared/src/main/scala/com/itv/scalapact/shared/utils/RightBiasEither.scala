package com.itv.scalapact.shared.utils

object RightBiasEither {

  implicit class RightBiasEither[AA, BB](val e: Either[AA, BB]) extends AnyVal {

    def bind[CC](f: BB => Either[AA, CC]): Either[AA, CC] =
      e match {
        case Right(bb) => f(bb)
        case Left(aa)  => Left(aa)
      }

    def map[CC](f: BB => CC): Either[AA, CC] =
      e match {
        case Right(bb) => Right(f(bb))
        case Left(aa)  => Left(aa)
      }

    def leftMap[CC](f: AA => CC): Either[CC, BB] =
      e match {
        case Right(bb) => Right(bb)
        case Left(aa)  => Left(f(aa))
      }

  }

}
