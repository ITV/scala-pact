package com.itv.scalapact.plugin.common

import argonaut._
import Argonaut._
import com.itv.scalapactcore.{Interaction, InteractionRequest, InteractionResponse, MatchingRule}

import scalaz._
import Scalaz._

object InteractionMatchers {

  lazy val matchRequest: List[Interaction] => InteractionRequest => \/[String, Interaction] = interactions => received =>
    interactions.find { ir =>
      matchMethods(ir.request.method.orElse(Option("GET")))(received.method) &&
        matchHeaders(ir.request.matchingRules)(ir.request.headers)(received.headers) &&
        matchPaths(ir.request.path.orElse(Option("/")))(ir.request.query)(received.path)(received.query) &&
        matchBodies(received.headers)(ir.request.body)(received.body)
    } match {
      case Some(matching) => matching.right
      case None => ("No matching request for: " + received).left
    }

  lazy val matchResponse: List[Interaction] => InteractionResponse => \/[String, Interaction] = interactions => received =>
    interactions.find{ ir =>
      matchStatusCodes(ir.response.status)(received.status) &&
        matchHeaders(ir.request.matchingRules)(ir.response.headers)(received.headers) &&
        matchBodies(received.headers)(ir.response.body)(received.body)
    } match {
      case Some(matching) => matching.right
      case None => ("No matching response for: " + received).left
    }

  lazy val matchStatusCodes: Option[Int] => Option[Int] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: Int, r: Int) => e == r)

  lazy val matchMethods: Option[String] => Option[String] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: String, r: String) => e.toUpperCase == r.toUpperCase)

  lazy val matchHeaders: Option[Map[String, MatchingRule]] => Option[Map[String, String]] => Option[Map[String, String]] => Boolean = matchingRules => expected => received => {

    val legalCharSeparators = List('(',')','<','>','@',',',';',':','\\','"','/','[',']','?','=','{','}')

    val correctSeparators: Char => String => String = separator => input =>
      input.split(separator).toList.map(_.trim).mkString(separator.toString)

    @annotation.tailrec
    def trimAllSeparators(separators: List[Char], input: String): String = {
      separators match {
        case Nil => input
        case x :: xs => trimAllSeparators(xs, correctSeparators(x)(input))
      }
    }

    val predicate = (matchingRules: Option[Map[String, MatchingRule]]) => (e: Map[String, String], r: Map[String, String]) => {

      def standardise(input: (String, String)): (String, String) = {
        (input._1.toLowerCase, trimAllSeparators(legalCharSeparators, input._2))
      }

      val withRules = matchingRules.map { mr =>
        e.filterKeys(key => mr.exists(p => p._1 == key))
      }.getOrElse(Map.empty[String, String])

      val withRuleMatchResult: Boolean = withRules.map { header =>
        matchingRules
          .flatMap { rules => rules.find(p => p._1 == header._1) }
            .flatMap { rule =>
              rule._2.regex.flatMap { regex =>
                r.get(header._1).map(rec => rec.matches(regex))
              }
            }.getOrElse(true)
      }.forall(_ == true)

      val noRules = e.filterKeys(k => !withRules.contains(k))

      val noRuleMatchResult: Boolean = noRules.map(p => standardise(p))
        .toSet
        .subsetOf(r.map(p => standardise(p)).toSet)

      noRuleMatchResult && withRuleMatchResult
    }

    generalMatcher(expected, received, predicate(matchingRules))
  }

  lazy val matchPaths: Option[String] => Option[String] => Option[String] => Option[String] => Boolean = expectedPath => expectedQuery => receivedPath => receivedQuery => {
    val expected = constructPath(expectedPath)(expectedQuery)
    val received = constructPath(receivedPath)(receivedQuery)

    generalMatcher(expected, received, (e: String, r: String) => {
      val ex = toPathStructure(e)
      val re = toPathStructure(r)

      ex.path == re.path && equalListsOfTuples(ex.params, re.params)
    })
  }

  lazy val matchBodies: Option[Map[String, String]] => Option[String] => Option[String] => Boolean = receivedHeaders => expected => received =>
    receivedHeaders match {
      case Some(hs) if hs.exists(p => p._1.toLowerCase == "content-type" && p._2.contains("json")) =>
        generalMatcher(expected, received, (e: String, r: String) => e.parse === r.parse) //TODO: Known issue, lists must be in the same order.

      case Some(hs) if hs.exists(p => p._1.toLowerCase == "content-type" && p._2.contains("xml")) =>
        generalMatcher(expected, received, (e: String, r: String) => e == r) //TODO: How shall we test equality of XML?

      case _ =>
        generalMatcher(expected, received, (e: String, r: String) => e == r)
    }

  private def generalMatcher[A](expected: Option[A], received: Option[A], predictate: (A, A) => Boolean): Boolean =
    (expected, received) match {
      case (None, None) => true
      case (None, Some(r)) => true
      case (Some(e), None) => false
      case (Some(e), Some(r)) => predictate(e, r)
    }

  private lazy val constructPath: Option[String] => Option[String] => Option[String] = path => query => Option {
    path.getOrElse("").split('?').toList ++ List(query.getOrElse("")) match {
      case Nil => "/"
      case x :: xs => List(x, xs.filter(!_.isEmpty).mkString("&")).mkString("?")
    }
  }

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

case class PathStructure(path: String, params: List[(String, String)])
