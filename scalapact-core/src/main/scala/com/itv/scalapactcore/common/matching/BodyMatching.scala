package com.itv.scalapactcore.common.matching

import com.itv.scalapact.shared.matchir._
import com.itv.scalapact.shared.json.JsonConversionFunctions

object BodyMatching {

  def nodeMatchToMatchResult(irNodeEqualityResult: IrNodeEqualityResult, rules: IrNodeMatchingRules, isXml: Boolean): MatchOutcome =
    irNodeEqualityResult match {
      case IrNodesEqual =>
        MatchOutcomeSuccess

      case e: IrNodesNotEqual =>
        MatchOutcomeFailed(e.renderDifferencesListWithRules(rules, isXml), e.differences.length * 1)
    }

  def matchBodies(headers: Option[Map[String, String]], expected: Option[String], received: Option[String])(implicit rules: IrNodeMatchingRules): MatchOutcome = {
    (expected, received) match {
      case (Some(ee), Some(rr)) if ee.nonEmpty && hasJsonHeader(headers) || stringIsProbablyJson(ee) && stringIsProbablyJson(rr) =>
        val predicate: (String, String) => MatchOutcome = (e, r) =>
          MatchIr.fromJSON(JsonConversionFunctions.fromJSON)(e).flatMap { ee =>
            MatchIr.fromJSON(JsonConversionFunctions.fromJSON)(r).map { rr =>
              nodeMatchToMatchResult(ee =~ rr, rules, isXml = false)
            }
          }.getOrElse(MatchOutcomeFailed("Failed to parse JSON body", 50))

        GeneralMatcher.generalMatcher(expected, received, MatchOutcomeFailed("Body mismatch", 50), predicate)

      case (Some(ee), Some(rr)) if ee.nonEmpty && hasXmlHeader(headers) || stringIsProbablyXml(ee) && stringIsProbablyXml(rr) =>
        val predicate: (String, String) => MatchOutcome = (e, r) =>
          MatchIr.fromXmlString(e).flatMap { ee =>
            MatchIr.fromXmlString(r).map { rr =>
              nodeMatchToMatchResult(ee =~ rr, rules, isXml = true)
            }
          }.getOrElse(MatchOutcomeFailed("Failed to parse XML body", 50))

        GeneralMatcher.generalMatcher(expected, received, MatchOutcomeFailed("Body mismatch", 50), predicate)

      case _ =>
        GeneralMatcher.generalMatcher(expected, received, MatchOutcomeFailed("Body mismatch", 50), (e: String, r: String) => PlainTextEquality.checkOutcome(e, r))
    }
  }

  def matchBodiesStrict(headers: Option[Map[String, String]], expected: Option[String], received: Option[String], bePermissive: Boolean)(implicit rules: IrNodeMatchingRules): MatchOutcome = {
    implicit val permissivity: IrNodeMatchPermissivity = if(bePermissive) Permissive else NonPermissive

    (expected, received) match {
      case (Some(ee), Some(rr)) if ee.nonEmpty && hasJsonHeader(headers) || stringIsProbablyJson(ee) && stringIsProbablyJson(rr) =>
        val predicate: (String, String) => MatchOutcome = (e, r) =>
          MatchIr.fromJSON(JsonConversionFunctions.fromJSON)(e).flatMap { ee =>
            MatchIr.fromJSON(JsonConversionFunctions.fromJSON)(r).map { rr =>
              nodeMatchToMatchResult(ee =<>= rr, rules, isXml = false)
            }
          }.getOrElse(MatchOutcomeFailed("Failed to parse JSON body", 50))

        GeneralMatcher.generalMatcher(expected, received, MatchOutcomeFailed("Body mismatch", 50), predicate)

      case (Some(ee), Some(rr)) if ee.nonEmpty && hasXmlHeader(headers) || stringIsProbablyXml(ee) && stringIsProbablyXml(rr) =>
        val predicate: (String, String) => MatchOutcome = (e, r) =>
          MatchIr.fromXmlString(e).flatMap { ee =>
            MatchIr.fromXmlString(r).map { rr =>
              nodeMatchToMatchResult(ee =<>= rr, rules, isXml = true)
            }
          }.getOrElse(MatchOutcomeFailed("Failed to parse XML body", 50))

        GeneralMatcher.generalMatcher(expected, received, MatchOutcomeFailed("Body mismatch", 50), predicate)

      case _ =>
        GeneralMatcher.generalMatcher(expected, received, MatchOutcomeFailed("Body mismatch", 50), (e: String, r: String) => PlainTextEquality.checkOutcome(e, r))
    }
  }

  def hasXmlHeader(headers: Option[Map[String, String]]): Boolean =
    findContentTypeHeader(headers).map(_.toLowerCase.contains("xml")).exists(_ == true)//2.10 compat

  def hasJsonHeader(headers: Option[Map[String, String]]): Boolean =
    findContentTypeHeader(headers).map(_.toLowerCase.contains("json")).exists(_ == true)//2.10 compat

  def findContentTypeHeader(headers: Option[Map[String, String]]): Option[String] =
    headers.map { hm =>
      hm.find(p => p._1.toLowerCase == "content-type").map(_._2)
    }.toList.headOption.flatten

  lazy val stringIsProbablyJson: String => Boolean = str =>
    ((s: String) => s.nonEmpty && ((s.startsWith("{") && s.endsWith("}")) || (s.startsWith("[") && s.endsWith("]"))))(str.trim)

  lazy val stringIsProbablyXml: String => Boolean = str =>
    ((s: String) => s.nonEmpty && s.startsWith("<") && s.endsWith(">"))(str.trim)

}
