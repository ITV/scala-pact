package com.itv.scalapactcore.common.matching

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.matchir._
import com.itv.scalapact.shared.typeclasses.IPactReader

object MessageMatchers {

  case class OutcomeAndMessage(outcome: MatchOutcome, closestMatchingMessage: Message)

  def renderOutcome(outcome: Option[OutcomeAndMessage],
                    renderedOriginal: String,
                    subject: String): Either[String, Message] =
    outcome match {
      case None =>
        Left("Entirely failed to match, something went horribly wrong.")

      case Some(OutcomeAndMessage(f @ MatchOutcomeFailed(_, _), message)) =>
        Left(
          s"""Failed to match $subject
             | ...original
             |$renderedOriginal
             | ...closest match was...
             |${message.renderAsString}
             | ...Differences
             |${f.renderDifferences}
             """.stripMargin
        )

      case Some(OutcomeAndMessage(MatchOutcomeSuccess, message)) =>
        Right(message)

    }

  def matchSingleMessage(rules: Option[Map[String, MatchingRule]], expected: String, received: String)(
      implicit matchingRules: IrNodeMatchingRules,
      pactReader: IPactReader
  ): MatchOutcome =
    MatchIr
      .fromJSON(pactReader.fromJSON)(expected)
      .flatMap { e =>
        MatchIr
          .fromJSON(pactReader.fromJSON)(received)
          .map(r => {
            nodeMatchToMatchResult(e =~ r, matchingRules, isXml = false)
          })
      }
      .getOrElse(MatchOutcomeFailed("Failed to parse JSON body", 50))

}
