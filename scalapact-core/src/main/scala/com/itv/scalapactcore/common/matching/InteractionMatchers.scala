package com.itv.scalapactcore.common.matching

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

import com.itv.scalapact.shared.{Interaction, InteractionRequest, InteractionResponse, MatchingRule}
import com.itv.scalapactcore.common.Helpers
import com.itv.scalapactcore.common.matching.PathMatching.PathAndQuery
import com.itv.scalapactcore.common.matchir._

object InteractionMatchers {

  case class OutcomeAndInteraction(outcome: MatchOutcome, matchingInteraction: Option[Interaction])

  val RequestSubject: String = "request"
  val ResponseSubject: String = "response"

  def renderOutcome(outcome: Option[OutcomeAndInteraction], renderedOriginal: String, subject: String): Either[String, Interaction] = {
    outcome match {
      case None =>
        Left("Entirely failed to match, something went horribly wrong.")

      case Some(OutcomeAndInteraction(MatchOutcomeSuccess, None)) =>
        Left("Match but failed to return interaction, something went horribly wrong.")

      case Some(OutcomeAndInteraction(MatchOutcomeSuccess, Some(interaction))) =>
        Right(interaction)

      case Some(OutcomeAndInteraction(f @ MatchOutcomeFailed(_), None)) =>
        Left(
          s"""Failed to match (Nothing close found):
            | ...Differences
            |${f.renderDifferences}
          """.stripMargin
        )

      case Some(OutcomeAndInteraction(f @ MatchOutcomeFailed(_), Some(i))) =>
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

  private def matchOrFindClosestRequest(strictMatching: Boolean, interactions: List[Interaction], received: InteractionRequest): Option[OutcomeAndInteraction] = {
    def rec(strict: Boolean, remaining: List[Interaction], actual: InteractionRequest, fails: List[(MatchOutcomeFailed, Interaction)]): Option[OutcomeAndInteraction] = {
      remaining match {
        case Nil =>
          fails.sortBy(_._1.errorCount).headOption.map(f => OutcomeAndInteraction(f._1, Option(f._2)))

        case x :: xs =>
          matchSingleRequest(strict, x.request.matchingRules, x.request, actual) match {
            case success @ MatchOutcomeSuccess =>
              Option(OutcomeAndInteraction(success, Option(x)))

            case failure @ MatchOutcomeFailed(_) =>
              rec(strict, xs, actual, (failure, x) :: fails)
          }
      }
    }

    rec(strictMatching, interactions, received, Nil)
  }

  def matchRequest(strictMatching: Boolean, interactions: List[Interaction], received: InteractionRequest): Either[String, Interaction] =
    if(interactions.isEmpty) Left("No interactions to compare with.")
    else renderOutcome(matchOrFindClosestRequest(strictMatching, interactions, received), received.renderAsString, RequestSubject)

  def matchSingleRequest(strictMatching: Boolean, rules: Option[Map[String, MatchingRule]], expected: InteractionRequest, received: InteractionRequest): MatchOutcome =
    IrNodeMatchingRules.fromPactRules(rules) match {
      case Left(e) =>
        MatchOutcomeFailed(e)

      case Right(r) if strictMatching =>
        MethodMatching.matchMethods(expected.method, received.method) +
          PathMatching.matchPathsStrict(PathAndQuery(expected.path, expected.query), PathAndQuery(received.path, received.query)) +
          HeaderMatching.matchHeaders(rules, expected.headers, received.headers) +
          BodyMatching.matchBodiesStrict(expected.headers, expected.body, received.body, bePermissive = false)(r)

      case Right(r) =>
        MethodMatching.matchMethods(expected.method, received.method) +
          PathMatching.matchPaths(PathAndQuery(expected.path, expected.query), PathAndQuery(received.path, received.query)) +
          HeaderMatching.matchHeaders(rules, expected.headers, received.headers) +
          BodyMatching.matchBodies(expected.headers, expected.body, received.body)(r)
    }

  private def matchOrFindClosestResponse(strictMatching: Boolean, interactions: List[Interaction], received: InteractionResponse): Option[OutcomeAndInteraction] = {
    def rec(strict: Boolean, remaining: List[Interaction], actual: InteractionResponse, fails: List[(MatchOutcomeFailed, Interaction)]): Option[OutcomeAndInteraction] = {
      remaining match {
        case Nil =>
          fails.sortBy(_._1.errorCount).headOption.map(f => OutcomeAndInteraction(f._1, Option(f._2)))

        case x :: xs =>
          matchSingleResponse(strict, x.response.matchingRules, x.response, actual) match {
            case success @ MatchOutcomeSuccess =>
              Option(OutcomeAndInteraction(success, Option(x)))

            case failure @ MatchOutcomeFailed(_) =>
              rec(strict, xs, actual, (failure, x) :: fails)
          }
      }
    }

    rec(strictMatching, interactions, received, Nil)
  }

  def matchResponse(strictMatching: Boolean, interactions: List[Interaction]): InteractionResponse => Either[String, Interaction] = received =>
    if(interactions.isEmpty) Left("No interactions to compare with.")
    else renderOutcome(matchOrFindClosestResponse(strictMatching, interactions, received), received.renderAsString, ResponseSubject)

  def matchSingleResponse(strictMatching: Boolean, rules: Option[Map[String, MatchingRule]], expected: InteractionResponse, received: InteractionResponse): MatchOutcome =
    IrNodeMatchingRules.fromPactRules(rules) match {
      case Left(e) =>
        MatchOutcomeFailed(e)

      case Right(r) if strictMatching =>
        StatusMatching.matchStatusCodes(expected.status, received.status) +
          HeaderMatching.matchHeaders(rules, expected.headers, received.headers) +
          BodyMatching.matchBodiesStrict(expected.headers, expected.body, received.body, bePermissive = true)(r)

      case Right(r) =>
        StatusMatching.matchStatusCodes(expected.status, received.status) +
          HeaderMatching.matchHeaders(rules, expected.headers, received.headers) +
          BodyMatching.matchBodies(expected.headers, expected.body, received.body)(r)
    }

}

sealed trait GeneralMatcher {

  protected def generalMatcher[A, GenericSuccess, GenericFailure](expected: Option[A], received: Option[A], predicate: (A, A) => Boolean): Boolean =
    (expected, received) match {
      case (None, None) => true

      case (Some(null), Some(null)) => true
      case (None, Some(null)) => true
      case (Some(null), None) => true

      case (Some("null"), Some("null")) => true
      case (None, Some("null")) => true
      case (Some("null"), None) => true

      case (None, Some(_)) => true
      case (Some(_), None) => false
      case (Some(e), Some(r)) => predicate(e, r)
    }

  protected def generalOutcomeMatcher[A, Outcome, S <: Outcome, F <: Outcome](expected: Option[A], received: Option[A], defaultSuccess: S, defaultFailure: F, predicate: (A, A) => Outcome): Outcome =
    (expected, received) match {
      case (None, None) => defaultSuccess

      case (Some(null), Some(null)) => defaultSuccess
      case (None, Some(null)) => defaultSuccess
      case (Some(null), None) => defaultSuccess

      case (Some("null"), Some("null")) => defaultSuccess
      case (None, Some("null")) => defaultSuccess
      case (Some("null"), None) => defaultSuccess

      case (None, Some(_)) => defaultSuccess
      case (Some(_), None) => defaultFailure
      case (Some(e), Some(r)) => predicate(e, r)
    }

}

object StatusMatching extends GeneralMatcher {

  def matchStatusCodes(expected: Option[Int], received: Option[Int]): MatchOutcome =
    generalOutcomeMatcher(
      expected,
      received,
      MatchOutcomeSuccess,
      MatchOutcomeFailed("Status codes did not match"),
      (e: Int, r: Int) => if(e == r) MatchOutcomeSuccess else MatchOutcomeFailed(s"Status code '$e' did not match '$r'")
    )

}

object PathMatching extends GeneralMatcher {

  case class PathAndQuery(path: Option[String], query: Option[String])

  def matchPaths(expected: PathAndQuery, received: PathAndQuery): MatchOutcome = {
    val matches = matchPathsWithPredicate(expected, received) {
      (ex: PathStructure, re: PathStructure) => {
        ex.path == re.path && equalListsOfTuples(ex.params, re.params)
      }
    }

    if(matches) MatchOutcomeSuccess else MatchOutcomeFailed("Paths do not match")
  }

  def matchPathsStrict(expected: PathAndQuery, received: PathAndQuery): MatchOutcome = {
    val matches = matchPathsWithPredicate(expected, received) {
      (ex: PathStructure, re: PathStructure) => {
        ex.path == re.path && ex.params.length == re.params.length && equalListsOfTuples(ex.params, re.params)
      }
    }

    if(matches) MatchOutcomeSuccess else MatchOutcomeFailed("Paths do not match")
  }

  private def matchPathsWithPredicate(expected: PathAndQuery, received: PathAndQuery)(predicate: (PathStructure, PathStructure) => Boolean): Boolean =
    generalMatcher(
      constructPath(expected).map(toPathStructure), constructPath(received).map(toPathStructure), predicate
    )

  private lazy val constructPath: PathAndQuery => Option[String] = pathAndQuery => Option {
    pathAndQuery.path.getOrElse("").split('?').toList ++ List(pathAndQuery.query.map(q => URLDecoder.decode(q, StandardCharsets.UTF_8.name())).getOrElse("")) match {
      case Nil => "/"
      case x :: xs => List(x, xs.filter(!_.isEmpty).mkString("&")).mkString("?")
    }
  }

  case class PathStructure(path: String, params: List[(String, String)])

  private lazy val toPathStructure: String => PathStructure = fullPath =>
    if(fullPath.isEmpty) PathStructure("", Nil)
    else {
      fullPath.split('?').toList match {
        case Nil => PathStructure("", Nil) //should never happen
        case x :: Nil => PathStructure(x, Nil)
        case x :: xs =>

          val params: List[(String, String)] = Helpers.pairTuples(xs.mkString.split('&').toList.flatMap(p => p.split('=').toList))

          PathStructure(x, params)
      }
    }

  private def equalListsOfTuples(listA: List[(String, String)], listB: List[(String, String)]): Boolean = {
    @annotation.tailrec
    def rec(remaining: List[((String, String), Int)], compare: List[((String, String), Int)], equalSoFar: Boolean): Boolean = {
      if(!equalSoFar) false
      else {
        remaining match {
          case Nil => true
          case x :: xs =>
            rec(xs, compare, compare.exists(p => p._1._1 == x._1._1 && p._1._2 == x._1._2 && p._2 == x._2))
        }
      }
    }

    listA.groupBy(_._1)
      .map(p => rec(p._2.zipWithIndex, listB.groupBy(_._1).getOrElse(p._1, Nil).zipWithIndex, equalSoFar = true))
      .forall(_ == true)
  }

}

object MethodMatching extends GeneralMatcher {

  def matchMethods(expected: Option[String], received: Option[String]): MatchOutcome =
    generalOutcomeMatcher(
      expected,
      received,
      MatchOutcomeSuccess,
      MatchOutcomeFailed("Methods did not match"),
      (e: String, r: String) => if(e.toUpperCase == r.toUpperCase) MatchOutcomeSuccess else MatchOutcomeFailed(s"Method '${e.toUpperCase}' did not match '${r.toUpperCase}'")
    )

}

object HeaderMatching extends GeneralMatcher {

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

      val noRuleMatchResult: Boolean = noRules.map(p => standardise(p))
        .toSet
        .subsetOf(r.map(p => standardise(p)).toSet)

      if(noRuleMatchResult && withRuleMatchResult) MatchOutcomeSuccess else MatchOutcomeFailed("Headers did not match")
    }

    generalOutcomeMatcher(expected, received, MatchOutcomeSuccess, MatchOutcomeFailed("Headers did not match"), predicate(matchingRules))
  }

}

object BodyMatching extends GeneralMatcher {

  def nodeMatchToMatchResult(irNodeEqualityResult: IrNodeEqualityResult, rules: IrNodeMatchingRules, isXml: Boolean): MatchOutcome =
    irNodeEqualityResult match {
      case IrNodesEqual =>
        MatchOutcomeSuccess

      case e: IrNodesNotEqual =>
        MatchOutcomeFailed(e.renderDifferencesListWithRules(rules, isXml))
    }

  def matchBodies(headers: Option[Map[String, String]], expected: Option[String], received: Option[String])(implicit rules: IrNodeMatchingRules): MatchOutcome = {
    (expected, received) match {
      case (Some(ee), Some(rr)) if ee.nonEmpty && hasJsonHeader(headers) || stringIsProbablyJson(ee) && stringIsProbablyJson(rr) =>
        val predicate: (String, String) => MatchOutcome = (e, r) =>
          MatchIr.fromJSON(e).flatMap { ee =>
            MatchIr.fromJSON(r).map { rr =>
              nodeMatchToMatchResult(ee =~ rr, rules, isXml = false)
            }
          }.getOrElse(MatchOutcomeFailed("Failed to parse JSON body"))

        generalOutcomeMatcher(expected, received, MatchOutcomeSuccess, MatchOutcomeFailed("Body mismatch"), predicate)

      case (Some(ee), Some(rr)) if ee.nonEmpty && hasXmlHeader(headers) || stringIsProbablyXml(ee) && stringIsProbablyXml(rr) =>
        val predicate: (String, String) => MatchOutcome = (e, r) =>
          MatchIr.fromXml(e).flatMap { ee =>
            MatchIr.fromXml(r).map { rr =>
              nodeMatchToMatchResult(ee =~ rr, rules, isXml = true)
            }
          }.getOrElse(MatchOutcomeFailed("Failed to parse XML body"))

        generalOutcomeMatcher(expected, received, MatchOutcomeSuccess, MatchOutcomeFailed("Body mismatch"), predicate)

      case _ =>
        generalOutcomeMatcher(expected, received, MatchOutcomeSuccess, MatchOutcomeFailed("Body mismatch"), (e: String, r: String) => PlainTextEquality.checkOutcome(e, r))
    }
  }

  def matchBodiesStrict(headers: Option[Map[String, String]], expected: Option[String], received: Option[String], bePermissive: Boolean)(implicit rules: IrNodeMatchingRules): MatchOutcome = {
    implicit val permissivity: IrNodeMatchPermissivity = if(bePermissive) Permissive else NonPermissive

    (expected, received) match {
      case (Some(ee), Some(rr)) if ee.nonEmpty && hasJsonHeader(headers) || stringIsProbablyJson(ee) && stringIsProbablyJson(rr) =>
        val predicate: (String, String) => MatchOutcome = (e, r) =>
          MatchIr.fromJSON(e).flatMap { ee =>
            MatchIr.fromJSON(r).map { rr =>
              nodeMatchToMatchResult(ee =<>= rr, rules, isXml = false)
            }
          }.getOrElse(MatchOutcomeFailed("Failed to parse JSON body"))

        generalOutcomeMatcher(expected, received, MatchOutcomeSuccess, MatchOutcomeFailed("Body mismatch"), predicate)

      case (Some(ee), Some(rr)) if ee.nonEmpty && hasXmlHeader(headers) || stringIsProbablyXml(ee) && stringIsProbablyXml(rr) =>
        val predicate: (String, String) => MatchOutcome = (e, r) =>
          MatchIr.fromXml(e).flatMap { ee =>
            MatchIr.fromXml(r).map { rr =>
              nodeMatchToMatchResult(ee =<>= rr, rules, isXml = true)
            }
          }.getOrElse(MatchOutcomeFailed("Failed to parse XML body"))

        generalOutcomeMatcher(expected, received, MatchOutcomeSuccess, MatchOutcomeFailed("Body mismatch"), predicate)

      case _ =>
        generalOutcomeMatcher(expected, received, MatchOutcomeSuccess, MatchOutcomeFailed("Body mismatch"), (e: String, r: String) => PlainTextEquality.checkOutcome(e, r))
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
