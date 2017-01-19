package com.itv.scalapactcore.common.matching

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

import argonaut.Argonaut._
import com.itv.scalapactcore.common.Helpers
import com.itv.scalapactcore.common.matching.ScalaPactXmlEquality._
import com.itv.scalapactcore.common.matching.ScalaPactJsonEquality._
import com.itv.scalapactcore.{Interaction, InteractionRequest, InteractionResponse, MatchingRule}

import scala.xml._

object InteractionMatchers {

  lazy val matchRequest: Boolean => List[Interaction] => InteractionRequest => Either[String, Interaction] = strictMatching => interactions => received =>
    interactions.find { interaction =>
      InteractionMatchingPrograms
        .matchRequestProgram(interaction.request, received)
        .foldMap {
          if(strictMatching) MatchingInterpreters.Request.strict
          else MatchingInterpreters.Request.permissive
        }
        .success
    } match {
      case Some(matching) => Right(matching)
      case None => Left("No matching request for: " + received)
    }

  lazy val matchResponse: Boolean => List[Interaction] => InteractionResponse => Either[String, Interaction] = strictMatching => interactions => received =>
    interactions.find { interaction =>
      InteractionMatchingPrograms
        .matchResponseProgram(interaction.response, received)
        .foldMap {
          if(strictMatching) MatchingInterpreters.Response.strict
          else MatchingInterpreters.Response.permissive
        }
        .success
    } match {
      case Some(matching) => Right(matching)
      case None => Left("No matching response for: " + received)
    }

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
        val predicate = (e: String, r: String) =>
          e.parseOption.flatMap { ee => r.parseOption.map { rr =>
              (ee =~ rr)(matchingRules)
          }}.exists(_ == true) // Use exists instead of contains for backwards compatibility with 2.10

        generalMatcher(expected, received, predicate)

      case Some(str) if stringIsXml(str) =>
        val predicate = (e: String, r: String) =>
          safeStringToXml(e).flatMap { ee => safeStringToXml(r).map { rr =>
            (ee =~ rr)(matchingRules)
          }}.exists(_ == true) // Use exists instead of contains for backwards compatibility with 2.10

        generalMatcher(expected, received, predicate)

      case _ =>
        generalMatcher(expected, received, (e: String, r: String) => PlainTextEquality.check(e, r))
    }

  lazy val matchBodiesStrict: Boolean => BodyMatchingRules => Option[String] => Option[String] => Boolean = beSelectivelyPermissive => matchingRules => expected => received =>
    expected match {
      case Some(str) if stringIsJson(str) =>
        val predicate = (e: String, r: String) =>
          e.parseOption.flatMap { ee => r.parseOption.map { rr =>
            (ee =<>= rr)(beSelectivelyPermissive)(matchingRules)
          }}.exists(_ == true) // Use exists instead of contains for backwards compatibility with 2.10

        generalMatcher(expected, received, predicate)

      case Some(str) if stringIsXml(str) =>
        val predicate = (e: String, r: String) =>
          safeStringToXml(e).flatMap { ee => safeStringToXml(r).map { rr =>
            (ee =<>= rr)(beSelectivelyPermissive)(matchingRules)
          }}.exists(_ == true) // Use exists instead of contains for backwards compatibility with 2.10

        generalMatcher(expected, received, predicate)

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
