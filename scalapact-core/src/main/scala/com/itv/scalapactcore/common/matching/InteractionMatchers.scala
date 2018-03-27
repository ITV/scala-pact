package com.itv.scalapactcore.common.matching

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.matchir.IrNodeMatchingRules
import com.itv.scalapact.shared.typeclasses.IPactReader
import com.itv.scalapactcore.common.matching.PathMatching.PathAndQuery

object InteractionMatchers {

  case class OutcomeAndInteraction(outcome: MatchOutcome, closestMatchingInteraction: Interaction)

  val RequestSubject: String = "request"
  val ResponseSubject: String = "response"

  def renderOutcome(outcome: Option[OutcomeAndInteraction], renderedOriginal: String, subject: String): Either[String, Interaction] = {
    outcome match {
      case None =>
        Left("Entirely failed to match, something went horribly wrong.")

      case Some(OutcomeAndInteraction(MatchOutcomeSuccess, interaction)) =>
        Right(interaction)

      case Some(OutcomeAndInteraction(f @ MatchOutcomeFailed(_, _), i)) =>
        Left(
          s"""Failed to match $subject
             | ...original
             |$renderedOriginal
             | ...closest match was...
             |${if(subject == RequestSubject) i.request.renderAsString else i.response.renderAsString }
             | ...Differences
             |${f.renderDifferences}
             """.stripMargin
        )
    }
  }

  def matchOrFindClosestRequest(strictMatching: Boolean, interactions: List[Interaction], received: InteractionRequest)(implicit pactReader: IPactReader): Option[OutcomeAndInteraction] = {
    def rec(strict: Boolean, remaining: List[Interaction], actual: InteractionRequest, fails: List[(MatchOutcomeFailed, Interaction)]): Option[OutcomeAndInteraction] = {
      remaining match {
        case Nil =>
          fails.sortBy(_._1.drift).headOption.map(f => OutcomeAndInteraction(f._1, f._2))

        case x :: xs =>
          matchSingleRequest(strict, x.request.matchingRules, x.request, actual) match {
            case success @ MatchOutcomeSuccess =>
              Option(OutcomeAndInteraction(success, x))

            case failure @ MatchOutcomeFailed(_, _) =>
              rec(strict, xs, actual, (failure, x) :: fails)
          }
      }
    }

    rec(strictMatching, interactions, received, Nil)
  }

  def matchRequest(strictMatching: Boolean, interactions: List[Interaction], received: InteractionRequest)(implicit pactReader: IPactReader): Either[String, Interaction] =
    if(interactions.isEmpty) Left("No interactions to compare with.")
    else renderOutcome(matchOrFindClosestRequest(strictMatching, interactions, received), received.renderAsString, RequestSubject)

  def matchSingleRequest(strictMatching: Boolean, rules: Option[Map[String, MatchingRule]], expected: InteractionRequest, received: InteractionRequest)(implicit pactReader: IPactReader): MatchOutcome =
    IrNodeMatchingRules.fromPactRules(rules) match {
      case Left(e) =>
        MatchOutcomeFailed(e)

      case Right(r) if strictMatching =>
        MethodMatching.matchMethods(expected.method, received.method) +
          PathMatching.matchPathsStrict(PathAndQuery(expected.path, expected.query), PathAndQuery(received.path, received.query)) +
          HeaderMatching.matchHeaders(rules, expected.headers, received.headers) +
          BodyMatching.matchBodiesStrict(expected.headers, expected.body, received.body, bePermissive = false)(r, pactReader)

      case Right(r) =>
        MethodMatching.matchMethods(expected.method, received.method) +
          PathMatching.matchPaths(PathAndQuery(expected.path, expected.query), PathAndQuery(received.path, received.query)) +
          HeaderMatching.matchHeaders(rules, expected.headers, received.headers) +
          BodyMatching.matchBodies(expected.headers, expected.body, received.body)(r, pactReader)
    }

  def matchOrFindClosestResponse(strictMatching: Boolean, interactions: List[Interaction], received: InteractionResponse)(implicit pactReader: IPactReader): Option[OutcomeAndInteraction] = {
    def rec(strict: Boolean, remaining: List[Interaction], actual: InteractionResponse, fails: List[(MatchOutcomeFailed, Interaction)]): Option[OutcomeAndInteraction] = {
      remaining match {
        case Nil =>
          fails.sortBy(_._1.drift).headOption.map(f => OutcomeAndInteraction(f._1, f._2))

        case x :: xs =>
          matchSingleResponse(strict, x.response.matchingRules, x.response, actual) match {
            case success @ MatchOutcomeSuccess =>
              Option(OutcomeAndInteraction(success, x))

            case failure @ MatchOutcomeFailed(_, _) =>
              rec(strict, xs, actual, (failure, x) :: fails)
          }
      }
    }

    rec(strictMatching, interactions, received, Nil)
  }

  def matchResponse(strictMatching: Boolean, interactions: List[Interaction])(implicit pactReader: IPactReader): InteractionResponse => Either[String, Interaction] = received =>
    if(interactions.isEmpty) Left("No interactions to compare with.")
    else renderOutcome(matchOrFindClosestResponse(strictMatching, interactions, received), received.renderAsString, ResponseSubject)

  def matchSingleResponse(strictMatching: Boolean, rules: Option[Map[String, MatchingRule]], expected: InteractionResponse, received: InteractionResponse)(implicit pactReader: IPactReader): MatchOutcome =
    IrNodeMatchingRules.fromPactRules(rules) match {
      case Left(e) =>
        MatchOutcomeFailed(e)

      case Right(r) if strictMatching =>
        StatusMatching.matchStatusCodes(expected.status, received.status) +
          HeaderMatching.matchHeaders(rules, expected.headers, received.headers) +
          BodyMatching.matchBodiesStrict(expected.headers, expected.body, received.body, bePermissive = true)(r, pactReader)

      case Right(r) =>
        StatusMatching.matchStatusCodes(expected.status, received.status) +
          HeaderMatching.matchHeaders(rules, expected.headers, received.headers) +
          BodyMatching.matchBodies(expected.headers, expected.body, received.body)(r, pactReader)
    }

}
