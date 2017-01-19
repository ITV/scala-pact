package com.itv.scalapactcore.common.matching

import com.itv.scalapactcore.common.matching.PathMatching.PathAndQuery

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

sealed trait MatchResponseStageA[A]

case class MatchResponseStatus(expected: InteractionResponse, actual: InteractionResponse) extends MatchResponseStageA[MatchStatusOutcome]
case class MatchResponseHeaders(expected: InteractionResponse, actual: InteractionResponse) extends MatchResponseStageA[MatchHeadersOutcome]
case class MatchResponseBody(expected: InteractionResponse, actual: InteractionResponse) extends MatchResponseStageA[MatchBodyOutcome]


sealed trait MatchRequestStageA[A]

case class MatchRequestMethod(expected: InteractionRequest, actual: InteractionRequest) extends MatchRequestStageA[MatchMethodOutcome]
case class MatchRequestPath(expected: InteractionRequest, actual: InteractionRequest) extends MatchRequestStageA[MatchPathOutcome]
case class MatchRequestHeaders(expected: InteractionRequest, actual: InteractionRequest) extends MatchRequestStageA[MatchHeadersOutcome]
case class MatchRequestBody(expected: InteractionRequest, actual: InteractionRequest) extends MatchRequestStageA[MatchBodyOutcome]

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

  type MatchResponseStage[A] = Free[MatchResponseStageA, A]

  // Response
  private def matchResponseStatus(expected: InteractionResponse, actual: InteractionResponse): MatchResponseStage[MatchStatusOutcome] =
    Free.liftF(MatchResponseStatus(expected, actual))

  private def matchResponseHeaders(expected: InteractionResponse, actual: InteractionResponse): MatchResponseStage[MatchHeadersOutcome] =
    Free.liftF(MatchResponseHeaders(expected, actual))

  private def matchResponseBody(expected: InteractionResponse, actual: InteractionResponse): MatchResponseStage[MatchBodyOutcome] =
    Free.liftF(MatchResponseBody(expected, actual))

  def matchResponseProgram(expected: InteractionResponse, actual: InteractionResponse): MatchResponseStage[ResponseMatchOutcome] =
    for {
      s <- matchResponseStatus(expected, actual)
      h <- matchResponseHeaders(expected, actual)
      b <- matchResponseBody(expected, actual)
    } yield ResponseMatchOutcome(s, h, b)

  type MatchRequestStage[A] = Free[MatchRequestStageA, A]

  // Request
  private def matchRequestMethod(expected: InteractionRequest, actual: InteractionRequest): MatchRequestStage[MatchMethodOutcome] =
    Free.liftF(MatchRequestMethod(expected, actual))

  private def matchRequestPath(expected: InteractionRequest, actual: InteractionRequest): MatchRequestStage[MatchPathOutcome] =
    Free.liftF(MatchRequestPath(expected, actual))

  private def matchRequestHeaders(expected: InteractionRequest, actual: InteractionRequest): MatchRequestStage[MatchHeadersOutcome] =
    Free.liftF(MatchRequestHeaders(expected, actual))

  private def matchRequestBody(expected: InteractionRequest, actual: InteractionRequest): MatchRequestStage[MatchBodyOutcome] =
    Free.liftF(MatchRequestBody(expected, actual))

  def matchRequestProgram(expected: InteractionRequest, actual: InteractionRequest): MatchRequestStage[RequestMatchOutcome] =
    for {
      m <- matchRequestMethod(expected, actual)
      p <- matchRequestPath(expected, actual)
      h <- matchRequestHeaders(expected, actual)
      b <- matchRequestBody(expected, actual)
    } yield RequestMatchOutcome(m, p, h, b)

}

object MatchingInterpreters {

  object Request {

    object permissive extends (MatchRequestStageA ~> Id) {

      def apply[A](stage: MatchRequestStageA[A]): Id[A] = {
        stage match {
          case MatchRequestMethod(expected, actual) =>
            if(MethodMatching.matchMethods(expected.method)(actual.method)) MatchMethodSuccess
            else MatchMethodFailed

          case MatchRequestPath(expected, actual) =>
            if(PathMatching.matchPaths(PathAndQuery(expected.path, expected.query))(PathAndQuery(actual.path, actual.query))) MatchPathSuccess
            else MatchPathFailed

          case MatchRequestHeaders(expected, actual) =>
            if(HeaderMatching.matchHeaders(expected.matchingRules)(expected.headers)(actual.headers)) MatchHeadersSuccess
            else MatchHeadersFailed

          case MatchRequestBody(expected, actual) =>
            if(BodyMatching.matchBodies(expected.matchingRules)(expected.body)(actual.body)) MatchBodySuccess
            else MatchBodyFailed

        }
      }

    }

    object strict extends (MatchRequestStageA ~> Id) {

      def apply[A](stage: MatchRequestStageA[A]): Id[A] = {
        stage match {
          case MatchRequestMethod(expected, actual) =>
            if(MethodMatching.matchMethods(expected.method)(actual.method)) MatchMethodSuccess
            else MatchMethodFailed

          case MatchRequestPath(expected, actual) =>
            if(PathMatching.matchPathsStrict(PathAndQuery(expected.path, expected.query))(PathAndQuery(actual.path, actual.query))) MatchPathSuccess
            else MatchPathFailed

          case MatchRequestHeaders(expected, actual) =>
            if(HeaderMatching.matchHeaders(expected.matchingRules)(expected.headers)(actual.headers)) MatchHeadersSuccess
            else MatchHeadersFailed

          case MatchRequestBody(expected, actual) =>
            if(BodyMatching.matchBodiesStrict(false)(expected.matchingRules)(expected.body)(actual.body)) MatchBodySuccess
            else MatchBodyFailed

        }
      }

    }

  }

  object Response {

    object permissive extends (MatchResponseStageA ~> Id) {

      def apply[A](stage: MatchResponseStageA[A]): Id[A] = {
        stage match {
          case MatchResponseStatus(expected, actual) =>
            if(StatusMatching.matchStatusCodes(expected.status)(actual.status)) MatchStatusSuccess
            else MatchStatusFailed

          case MatchResponseHeaders(expected, actual) =>
            if(HeaderMatching.matchHeaders(expected.matchingRules)(expected.headers)(actual.headers)) MatchHeadersSuccess
            else MatchHeadersFailed

          case MatchResponseBody(expected, actual) =>
            if(BodyMatching.matchBodies(expected.matchingRules)(expected.body)(actual.body)) MatchBodySuccess
            else MatchBodyFailed

        }
      }

    }

    object strict extends (MatchResponseStageA ~> Id) {

      def apply[A](stage: MatchResponseStageA[A]): Id[A] = {
        stage match {
          case MatchResponseStatus(expected, actual) =>
            if(StatusMatching.matchStatusCodes(expected.status)(actual.status)) MatchStatusSuccess
            else MatchStatusFailed

          case MatchResponseHeaders(expected, actual) =>
            if(HeaderMatching.matchHeaders(expected.matchingRules)(expected.headers)(actual.headers)) MatchHeadersSuccess
            else MatchHeadersFailed

          case MatchResponseBody(expected, actual) =>
            if(BodyMatching.matchBodiesStrict(true)(expected.matchingRules)(expected.body)(actual.body)) MatchBodySuccess
            else MatchBodyFailed

        }
      }

    }

  }

}
