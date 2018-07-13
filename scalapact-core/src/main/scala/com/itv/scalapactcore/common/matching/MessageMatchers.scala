package com.itv.scalapactcore.common.matching

import com.itv.scalapact.shared.matchir._
import com.itv.scalapact.shared.{MatchingRule, Message}
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

  def matchSingleMessage(expected: String, received: String, rules: Message.MatchingRules, strict: Boolean)(
      implicit pactReader: IPactReader
  ): MatchOutcome = {

    def evaluateRule(rules: (String, MatchingRule)): MatchOutcome =
      IrNodeMatchingRules.fromPactRules(Some(Map(rules))) match {
        case Left(e) =>
          MatchOutcomeFailed(e)
        case Right(matchingRules) =>
          val result = for {
            ee <- MatchIr.fromJSON(pactReader.fromJSON)(expected)
            rr <- MatchIr.fromJSON(pactReader.fromJSON)(received)
          } yield
            nodeMatchToMatchResult(ee.isEqualTo(rr, strict = strict, matchingRules, bePermissive = !strict),
                                   matchingRules,
                                   isXml = false)

          result.getOrElse(MatchOutcomeFailed("Failed to parse JSON body", 50))

      }

    rules
      .get("body")
      .toList
      .flatMap {
        _.map { case (k, v) => k.replace("$", "$.body") -> v.matchers }
      }
      .flatMap(x => x._2.map(y => x._1 -> y))
      .foldLeft[MatchOutcome](MatchOutcomeSuccess) { (acc, next: (String, MatchingRule)) =>
        acc + evaluateRule(next)
      }
  }

}
