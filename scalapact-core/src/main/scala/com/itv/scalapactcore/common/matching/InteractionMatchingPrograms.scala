package com.itv.scalapactcore.common.matching

import scalaz._
import Scalaz._

import com.itv.scalapactcore.{InteractionRequest, InteractionResponse}

sealed trait MatchStatusOutcome
case object MatchStatusSuccess extends MatchStatusOutcome
case object MatchStatusFailed extends MatchStatusOutcome

sealed trait MatchHeadersOutcome
case object MatchHeadersSuccess extends MatchHeadersOutcome
case object MatchHeadersFailed extends MatchHeadersOutcome //TODO: What kind of match failures do we get that we can model?

sealed trait MatchBodyOutcome
case object MatchBodySuccess extends MatchBodyOutcome
case object MatchBodyFailed extends MatchBodyOutcome //TODO: What kind of match failures do we get that we can model?

sealed trait MatchMethodOutcome
case object MatchMethodSuccess extends MatchMethodOutcome
case object MatchMethodFailed extends MatchMethodOutcome //TODO: What kind of match failures do we get that we can model?

sealed trait MatchPathOutcome
case object MatchPathSuccess extends MatchPathOutcome
case object MatchPathFailed extends MatchPathOutcome //TODO: What kind of match failures do we get that we can model?

sealed trait MatchingStageA[A]

case class MatchResponseStatus(expected: InteractionResponse, actual: InteractionResponse) extends MatchingStageA[MatchStatusOutcome]
case class MatchResponseHeaders(expected: InteractionResponse, actual: InteractionResponse) extends MatchingStageA[MatchHeadersOutcome]
case class MatchResponseBody(expected: InteractionResponse, actual: InteractionResponse) extends MatchingStageA[MatchBodyOutcome]

case class MatchRequestMethod(expected: InteractionRequest, actual: InteractionRequest) extends MatchingStageA[MatchMethodOutcome]
case class MatchRequestPath(expected: InteractionRequest, actual: InteractionRequest) extends MatchingStageA[MatchPathOutcome]
case class MatchRequestHeaders(expected: InteractionRequest, actual: InteractionRequest) extends MatchingStageA[MatchHeadersOutcome]
case class MatchRequestBody(expected: InteractionRequest, actual: InteractionRequest) extends MatchingStageA[MatchBodyOutcome]

case class RequestMatchOutcome(method: MatchMethodOutcome, path: MatchPathOutcome, headers: MatchHeadersOutcome, body: MatchBodyOutcome) {
  val success: Boolean = {
    (method, path, headers, body) match {
      case (MatchMethodSuccess, MatchPathSuccess, MatchHeadersSuccess, MatchBodySuccess) => true
      case _ => false
    }
  }

  val failed: Boolean = !success
}

case class ResponseMatchOutcome(status: MatchStatusOutcome, headers: MatchHeadersOutcome, body: MatchBodyOutcome) {
  val success: Boolean = {
    (status, headers, body) match {
      case (MatchStatusSuccess, MatchHeadersSuccess, MatchBodySuccess) => true
      case _ => false
    }
  }

  val failed: Boolean = !success
}

object InteractionMatchingPrograms {

  type MatchingStage[A] = Free[MatchingStageA, A]

  // Response
  private def matchResponseStatus(expected: InteractionResponse, actual: InteractionResponse): MatchingStage[MatchStatusOutcome] =
    Free.liftF(MatchResponseStatus(expected, actual))

  private def matchResponseHeaders(expected: InteractionResponse, actual: InteractionResponse): MatchingStage[MatchHeadersOutcome] =
    Free.liftF(MatchResponseHeaders(expected, actual))

  private def matchResponseBody(expected: InteractionResponse, actual: InteractionResponse): MatchingStage[MatchBodyOutcome] =
    Free.liftF(MatchResponseBody(expected, actual))

  def matchResponseProgram(expected: InteractionResponse, actual: InteractionResponse): MatchingStage[ResponseMatchOutcome] =
    for {
      s <- matchResponseStatus(expected, actual)
      h <- matchResponseHeaders(expected, actual)
      b <- matchResponseBody(expected, actual)
    } yield ResponseMatchOutcome(s, h, b)

  // Request
  private def matchRequestMethod(expected: InteractionRequest, actual: InteractionRequest): MatchingStage[MatchMethodOutcome] =
    Free.liftF(MatchRequestMethod(expected, actual))

  private def matchRequestPath(expected: InteractionRequest, actual: InteractionRequest): MatchingStage[MatchPathOutcome] =
    Free.liftF(MatchRequestPath(expected, actual))

  private def matchRequestHeaders(expected: InteractionRequest, actual: InteractionRequest): MatchingStage[MatchHeadersOutcome] =
    Free.liftF(MatchRequestHeaders(expected, actual))

  private def matchRequestBody(expected: InteractionRequest, actual: InteractionRequest): MatchingStage[MatchBodyOutcome] =
    Free.liftF(MatchRequestBody(expected, actual))

  def matchRequestProgram(expected: InteractionRequest, actual: InteractionRequest): MatchingStage[RequestMatchOutcome] =
    for {
      m <- matchRequestMethod(expected, actual)
      p <- matchRequestPath(expected, actual)
      h <- matchRequestHeaders(expected, actual)
      b <- matchRequestBody(expected, actual)
    } yield RequestMatchOutcome(m, p, h, b)

}

object InteractionMatchingInterpreters {

  def permissiveResponseInterpreter: MatchingStageA ~> Id  =
    new (MatchingStageA ~> Id) {

      def apply[A](fa: MatchingStageA[A]): Id[A] =
        fa match {
          case MatchResponseStatus(expected, actual) =>
            if(StatusMatching.matchStatusCodes(expected.status)(actual.status)) MatchStatusSuccess.asInstanceOf[A]
            else MatchStatusFailed.asInstanceOf[A]

          case MatchResponseHeaders(expected, actual) =>
            if(HeaderMatching.matchHeaders(expected.matchingRules)(expected.headers)(actual.headers)) MatchHeadersSuccess.asInstanceOf[A]
            else MatchHeadersFailed.asInstanceOf[A]

          case MatchResponseBody(expected, actual) =>
            if(BodyMatching.matchBodies(expected.matchingRules)(expected.body)(actual.body)) MatchBodySuccess.asInstanceOf[A]
            else MatchBodyFailed.asInstanceOf[A]

        }

    }

  def strictResponseInterpreter: MatchingStageA ~> Id  =
    new (MatchingStageA ~> Id) {

      def apply[A](fa: MatchingStageA[A]): Id[A] =
        fa match {
          case MatchResponseStatus(expected, actual) =>
            if(StatusMatching.matchStatusCodes(expected.status)(actual.status)) MatchStatusSuccess.asInstanceOf[A]
            else MatchStatusFailed.asInstanceOf[A]

          case MatchResponseHeaders(expected, actual) =>
            if(HeaderMatching.matchHeaders(expected.matchingRules)(expected.headers)(actual.headers)) MatchHeadersSuccess.asInstanceOf[A]
            else MatchHeadersFailed.asInstanceOf[A]

          case MatchResponseBody(expected, actual) =>
            if(BodyMatching.matchBodiesStrict(true)(expected.matchingRules)(expected.body)(actual.body)) MatchBodySuccess.asInstanceOf[A]
            else MatchBodyFailed.asInstanceOf[A]

        }

    }

}
