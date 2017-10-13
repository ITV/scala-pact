package com.itv.scalapactcore.common.matching

import com.itv.scalapact.shared.MatchingRule

object HeaderMatching {

  def matchHeaders(matchingRules: Option[Map[String, MatchingRule]], expected: Option[Map[String, String]], received: Option[Map[String, String]]): MatchOutcome = {

    val legalCharSeparators = List('(',')','<','>','@',',',';',':','\\','"','/','[',']','?','=','{','}')

    val trimAllInstancesOfSeparator: Char => String => String = separator => input =>
      input.split(separator).toList.map(_.trim).mkString(separator.toString)

    @annotation.tailrec
    def trimAllSeparators(separators: List[Char], input: String): String = {
      separators match {
        case Nil => input
        case x :: xs => trimAllSeparators(xs, trimAllInstancesOfSeparator(x)(input))
      }
    }

    val predicate: Option[Map[String, MatchingRule]] => (Map[String, String], Map[String, String]) => MatchOutcome = matchingRules => (e, r) => {

      val strippedMatchingRules = matchingRules.map { mmr =>
        mmr
          .filter(mr => mr._1.startsWith("$.headers.") && mr._2.`match`.exists(_ == "regex")) //Use exists for 2.10 compat
          .map(mr => (mr._1.substring("$.headers.".length).toLowerCase, mr._2))
      }

      def standardise(input: (String, String)): (String, String) = {
        (input._1.toLowerCase, trimAllSeparators(legalCharSeparators, input._2))
      }

      val expectedHeadersWithMatchingRules = strippedMatchingRules
        .map { mr =>
          e.map(p => standardise(p)).filterKeys(key => mr.exists(p => p._1 == key))
        }
        .getOrElse(Map.empty[String, String])

      //TODO: Count mismatches.
      val withRuleMatchResult: Boolean = expectedHeadersWithMatchingRules.map { header =>
        strippedMatchingRules
          .flatMap { rules => rules.find(p => p._1 == header._1) } // Find the rule that matches the expected header
          .flatMap { rule =>
          rule._2.regex.flatMap { regex =>
            r.map(h => (h._1.toLowerCase, h._2)).get(header._1).map(rec => rec.matches(regex))
          }
        }
          .getOrElse(true)
      }.forall(_ == true)

      val noRules = e.map(p => standardise(p)).filterKeys(k => !expectedHeadersWithMatchingRules.contains(k))

      //TODO: Convert to recursion so you can count differences.
      val noRuleMatchResult: Boolean = noRules.map(p => standardise(p))
        .toSet
        .subsetOf(r.map(p => standardise(p)).toSet)

      if(noRuleMatchResult && withRuleMatchResult) MatchOutcomeSuccess else MatchOutcomeFailed("Headers did not match", 50)
    }

    GeneralMatcher.generalMatcher(expected, received, MatchOutcomeFailed("Headers did not match", 50), predicate(matchingRules))
  }

}