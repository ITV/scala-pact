package com.itv.scalapactcore.common.matching

import com.itv.scalapact.shared.MatchingRule
import com.itv.scalapact.shared.matchir.IrNodePath.IrNodePathEmpty
import com.itv.scalapact.shared.matchir.IrNodeRule.{IrNodeMinArrayLengthRule, IrNodeRegexRule, IrNodeTypeRule}
import com.itv.scalapact.shared.matchir._

import scala.annotation.tailrec

object HeaderMatching {

  val legalCharSeparators: List[Char] =
    List('(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}')

  def trimAllInstancesOfSeparator: Char => String => String =
    separator => input => input.split(separator).toList.map(_.trim).mkString(separator.toString)

  def standardiseHeader(input: (String, String)): (String, String) =
    (input._1.toLowerCase, trimAllSeparators(legalCharSeparators, input._2))

  @annotation.tailrec
  def trimAllSeparators(separators: List[Char], input: String): String =
    separators match {
      case Nil     => input
      case x :: xs => trimAllSeparators(xs, trimAllInstancesOfSeparator(x)(input))
    }

  def flattenMatchOutcomeList(l: List[MatchOutcome]): MatchOutcome =
    l match {
      case Nil =>
        MatchOutcomeSuccess

      case x :: xs =>
        xs.foldLeft(x)(_ + _)
    }

  def matchHeaders(
      matchingRules: Option[Map[String, MatchingRule]],
      expected: Option[Map[String, String]],
      received: Option[Map[String, String]]
  ): MatchOutcome = {

    val rules = findAndCleanupApplicableMatchingRules(matchingRules)

    val predicate: (Map[String, String], Map[String, String]) => MatchOutcome = (e, r) => {

      val (withRules, withoutRules) =
        e.partition { p =>
          rules.exists(mr => mr.exists(r => r._1 == "$.headers." + standardiseHeader(p)._1))
        }

      val withRuleMatchResult: MatchOutcome = {

        val outcomes = IrNodeMatchingRules.fromPactRules(rules) match {
          case Left(error) =>
            List(MatchOutcomeFailed(error))

          case Right(rls) =>
            withRules.toList
              .map(p => standardiseHeader(p))
              .flatMap { header =>
                rls
                  .findForPath(IrNodePathEmpty <~ header._1, isXml = false)
                  .map {
                    case IrNodeTypeRule(_) =>
                      MatchOutcomeSuccess

                    case IrNodeRegexRule(regex, _) =>
                      MatchOutcome.fromPredicate(
                        regex.r.findAllIn(header._2).nonEmpty,
                        s"Header '${header._1}' value of '${header._2}' did not match regex requirement for '$regex'",
                        1
                      )

                    case IrNodeMinArrayLengthRule(_, _) =>
                      MatchOutcomeSuccess
                  }
              }
        }

        flattenMatchOutcomeList(outcomes)
      }

      val noRuleMatchResult: MatchOutcome = {
        @tailrec
        def rec(
            remaining: List[(String, String)],
            received: List[(String, String)],
            acc: List[MatchOutcome]
        ): List[MatchOutcome] =
          remaining match {
            case Nil =>
              acc

            case x :: xs =>
              rec(
                xs,
                received,
                MatchOutcome.fromPredicate(
                  received.contains(standardiseHeader(x)),
                  s"Missing header called '${x._1}' with value '${x._2}'",
                  1
                ) :: acc
              )
          }

        flattenMatchOutcomeList(rec(withoutRules.toList, r.map(standardiseHeader).toList, Nil))
      }

      noRuleMatchResult + withRuleMatchResult
    }

    GeneralMatcher.generalMatcher(expected, received, MatchOutcomeFailed("Headers did not match", 50), predicate)
  }

  def findAndCleanupApplicableMatchingRules(
      matchingRules: Option[Map[String, MatchingRule]]
  ): Option[Map[String, MatchingRule]] =
    matchingRules.map { mmr =>
      mmr
        .filter(mr => mr._1.startsWith("$.headers."))
        .map(mr => (mr._1.toLowerCase, mr._2))
    }

}
