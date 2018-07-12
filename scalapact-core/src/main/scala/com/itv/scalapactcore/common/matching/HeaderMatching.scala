package com.itv.scalapactcore.common.matching

import com.itv.scalapact.shared.MatchingRule
import com.itv.scalapact.shared.matchir._

object HeaderMatching extends PrimitiveConversionFunctions {

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

  def matchHeaders(matchingRules: Option[Map[String, MatchingRule]],
                   expected: Option[Map[String, String]],
                   received: Option[Map[String, String]]): MatchOutcome = {

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
              .flatMap {
                case (key, value) =>
                  rls
                    .findForPath(IrNodePathEmpty <~ key, isXml = false)
                    .map {
                      _.fold[MatchOutcome](
                        _ => MatchOutcomeSuccess,
                        (regex, _) =>
                          MatchOutcome.fromPredicate(
                            regex.r.findAllIn(value).nonEmpty,
                            s"Header '$key' value of '$value' did not match regex requirement for '$regex'",
                            1
                        ),
                        (_, _) => MatchOutcomeSuccess,
                        _ => {
                          MatchOutcome
                            .fromPredicate(
                              r.get(key).exists(_.matches(isIntegerValueRegex)),
                              s"Header '$key' value of '$value' did not match to an integer",
                              1
                            )
                        },
                        _ =>
                          MatchOutcome
                            .fromPredicate(r.get(key).exists(_.matches(isDecimalValueRegex)),
                                           s"Header '$key' value of '$value' did not match to an decimal",
                                           1)
                      )
                    }
              }
        }

        flattenMatchOutcomeList(outcomes)
      }

      val noRuleMatchResult: MatchOutcome = {
        def rec(remaining: List[(String, String)],
                received: List[(String, String)],
                acc: List[MatchOutcome]): List[MatchOutcome] =
          remaining match {
            case Nil =>
              acc

            case x :: xs =>
              rec(
                xs,
                received,
                MatchOutcome.fromPredicate(
                  received.exists(_ == standardiseHeader(x)), // exists for 2.10 compat
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
