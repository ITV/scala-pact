package com.itv.scalapactcore.common.matching

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

import argonaut.Argonaut._
import com.itv.scalapactcore.common.Helpers
import com.itv.scalapactcore.common.matching.PermissiveXmlEquality._
import com.itv.scalapactcore.common.matching.ScalaPactJsonEquality._
import com.itv.scalapactcore.{Interaction, InteractionRequest, InteractionResponse, MatchingRule}

import scala.xml._
import scalaz.Scalaz._
import scalaz._

object InteractionMatchers {

  import StatusMatching._
  import PathMatching._
  import HeaderMatching._
  import MethodMatching._
  import BodyMatching._

  lazy val matchRequest: Boolean => List[Interaction] => InteractionRequest => \/[String, Interaction] = strictMatching => interactions => received =>
    interactions.find { ir =>
      if(strictMatching) isStrictRequestMatch(ir)(received)
      else isNonStrictRequestMatch(ir)(received)
    } match {
      case Some(matching) => matching.right
      case None => ("No matching request for: " + received).left
    }

  lazy val isStrictRequestMatch: Interaction => InteractionRequest => Boolean = ir => received =>
    matchMethods(ir.request.method.orElse(Option("GET")))(received.method) &&
      matchHeaders(ir.request.matchingRules)(ir.request.headers)(received.headers) &&
      matchPathsStrict(PathAndQuery(ir.request.path.orElse(Option("/")), ir.request.query))(PathAndQuery(received.path, received.query)) &&
      matchBodiesStrict(false)(ir.request.matchingRules)(ir.request.body)(received.body)

  lazy val isNonStrictRequestMatch: Interaction => InteractionRequest => Boolean = ir => received =>
    matchMethods(ir.request.method.orElse(Option("GET")))(received.method) &&
      matchHeaders(ir.request.matchingRules)(ir.request.headers)(received.headers) &&
      matchPaths(PathAndQuery(ir.request.path.orElse(Option("/")), ir.request.query))(PathAndQuery(received.path, received.query)) &&
      matchBodies(ir.request.matchingRules)(ir.request.body)(received.body)

  lazy val matchResponse: Boolean => List[Interaction] => InteractionResponse => \/[String, Interaction] = strictMatching => interactions => received =>
    interactions.find{ ir =>
      if(strictMatching) isStrictResponseMatch(ir)(received)
      else isNonStrictResponseMatch(ir)(received)
    } match {
      case Some(matching) => matching.right
      case None => ("No matching response for: " + received).left
    }

  lazy val isStrictResponseMatch: Interaction => InteractionResponse => Boolean = ir => received =>
    matchStatusCodes(ir.response.status)(received.status) &&
      matchHeaders(ir.response.matchingRules)(ir.response.headers)(received.headers) &&
      matchBodiesStrict(true)(ir.response.matchingRules)(ir.response.body)(received.body)

  lazy val isNonStrictResponseMatch: Interaction => InteractionResponse => Boolean = ir => received =>
    matchStatusCodes(ir.response.status)(received.status) &&
      matchHeaders(ir.response.matchingRules)(ir.response.headers)(received.headers) &&
      matchBodies(ir.response.matchingRules)(ir.response.body)(received.body)

}

sealed trait GeneralMatcher {

  protected def generalMatcher[A](expected: Option[A], received: Option[A], predicate: (A, A) => Boolean): Boolean =
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

}

object StatusMatching extends GeneralMatcher {

  lazy val matchStatusCodes: Option[Int] => Option[Int] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: Int, r: Int) => e == r)

}

object PathMatching extends GeneralMatcher {

  case class PathAndQuery(path: Option[String], query: Option[String])

  lazy val matchPaths: PathAndQuery => PathAndQuery => Boolean = expected => received =>
    matchPathsWithPredicate(expected)(received) {
      (ex: PathStructure, re: PathStructure) => {
        ex.path == re.path && equalListsOfTuples(ex.params, re.params)
      }
    }

  lazy val matchPathsStrict: PathAndQuery => PathAndQuery => Boolean = expected => received =>
    matchPathsWithPredicate(expected)(received) {
      (ex: PathStructure, re: PathStructure) => {
        ex.path == re.path && ex.params.length == re.params.length && equalListsOfTuples(ex.params, re.params)
      }
    }

  private lazy val matchPathsWithPredicate: PathAndQuery => PathAndQuery => ((PathStructure, PathStructure) => Boolean) => Boolean = expected => received => predicate =>
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

  lazy val matchMethods: Option[String] => Option[String] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: String, r: String) => e.toUpperCase == r.toUpperCase)

}

object HeaderMatching extends GeneralMatcher {

  type HeaderMatchingRules = Option[Map[String, MatchingRule]]

  lazy val matchHeaders: HeaderMatchingRules => Option[Map[String, String]] => Option[Map[String, String]] => Boolean = matchingRules => expected => received => {

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

    val predicate = (matchingRules: Option[Map[String, MatchingRule]]) => (e: Map[String, String], r: Map[String, String]) => {

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

      noRuleMatchResult && withRuleMatchResult
    }

    generalMatcher(expected, received, predicate(matchingRules))
  }

}

object BodyMatching extends GeneralMatcher {

  type BodyMatchingRules = Option[Map[String, MatchingRule]]

  lazy val matchBodies: BodyMatchingRules => Option[String] => Option[String] => Boolean = matchingRules => expected => received =>
    expected match {
      case Some(str) if stringIsJson(str) =>
        generalMatcher(expected, received, (e: String, r: String) => (e.parseOption |@| r.parseOption) { (e, r) => (e =~ r)(matchingRules) }.exists(_ == true)) // Use exists instead of contains for backwards compatibility with 2.10

      case Some(str) if stringIsXml(str) =>
        generalMatcher(expected, received, (e: String, r: String) => (safeStringToXml(e) |@| safeStringToXml(r)) { (e, r) => (e =~ r)(matchingRules) }.exists(_ == true)) // Use exists instead of contains for backwards compatibility with 2.10

      case _ =>
        generalMatcher(expected, received, (e: String, r: String) => PlainTextEquality.check(e, r))
    }

  lazy val matchBodiesStrict: Boolean => BodyMatchingRules => Option[String] => Option[String] => Boolean = beSelectivelyPermissive => matchingRules => expected => received =>
    expected match {
      case Some(str) if stringIsJson(str) =>
        generalMatcher(expected, received, (e: String, r: String) => (e.parseOption |@| r.parseOption) { (e, r) => (e =<>= r)(beSelectivelyPermissive)(matchingRules) }.exists(_ == true)) // Use exists instead of contains for backwards compatibility with 2.10

      case Some(str) if stringIsXml(str) =>
        generalMatcher(expected, received, (e: String, r: String) => (safeStringToXml(e) |@| safeStringToXml(r)) { (e, r) => (e =<>= r)(beSelectivelyPermissive)(matchingRules) }.exists(_ == true)) // Use exists instead of contains for backwards compatibility with 2.10

      case _ =>
        generalMatcher(expected, received, (e: String, r: String) => PlainTextEquality.check(e, r))
    }

  lazy val stringIsJson: String => Boolean = str => str.parseOption.isDefined
  lazy val stringIsXml: String => Boolean = str => safeStringToXml(str).isDefined

  lazy val safeStringToXml: String => Option[Elem] = str =>
    try {
      Option(XML.loadString(str))
    } catch {
      case _: Throwable => None
    }
}
