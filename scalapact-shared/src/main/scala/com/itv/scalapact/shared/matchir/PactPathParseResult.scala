package com.itv.scalapact.shared.matchir

import com.itv.scalapact.shared.matchir.PactPathParseResult.{PactPathParseFailure, PactPathParseSuccess}

sealed trait PactPathParseResult {
  def toOption: Option[IrNodePath] =
    this match {
      case PactPathParseSuccess(p) =>
        Option(p)

      case _ =>
        None
    }

  def toEither: Either[String, IrNodePath] =
    this match {
      case PactPathParseSuccess(p) =>
        Right(p)

      case e: PactPathParseFailure =>
        Left(e.errorString)
    }
}

object PactPathParseResult {

  final case class PactPathParseSuccess(irNodePath: IrNodePath) extends PactPathParseResult

  final case class PactPathParseFailure(original: String, lookingAt: String, specificError: Option[String])
      extends PactPathParseResult {
    def errorString: String =
      s"${specificError.getOrElse("Failed to parse PactPath")}. Was looking at '$lookingAt' in '$original'"
  }

}
