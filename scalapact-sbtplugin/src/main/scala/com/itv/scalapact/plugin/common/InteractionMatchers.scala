package com.itv.scalapact.plugin.common

import argonaut._
import Argonaut._
import com.itv.scalapactcore.{Interaction, InteractionResponse, InteractionRequest}

import scalaz._
import Scalaz._

object InteractionMatchers {

  lazy val matchRequest: List[Interaction] => InteractionRequest => \/[String, Interaction] = interactions => received =>
    interactions.find { ir =>
      matchMethods(ir.request.method.orElse(Option("GET")))(received.method) &&
        matchHeaders(ir.request.headers)(received.headers) &&
        matchPaths(ir.request.path.orElse(Option("/")))(received.path) &&
        matchBodies(received.headers)(ir.request.body)(received.body)
    } match {
      case Some(matching) => matching.right
      case None => ("No matching request for: " + received).left
    }

  lazy val matchResponse: List[Interaction] => InteractionResponse => \/[String, Interaction] = interactions => received =>
    interactions.find{ ir =>
      matchStatusCodes(ir.response.status)(received.status) &&
        matchHeaders(ir.response.headers)(received.headers) &&
        matchBodies(received.headers)(ir.response.body)(received.body)
    } match {
      case Some(matching) => matching.right
      case None => ("No matching response for: " + received).left
    }

  lazy val matchStatusCodes: Option[Int] => Option[Int] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: Int, r: Int) => e == r)

  lazy val matchMethods: Option[String] => Option[String] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: String, r: String) => e == r)

  lazy val matchHeaders: Option[Map[String, String]] => Option[Map[String, String]] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: Map[String, String], r: Map[String, String]) => e.toSet.subsetOf(r.toSet))

  lazy val matchPaths: Option[String] => Option[String] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: String, r: String) => toPathStructure(e) == toPathStructure(r))

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

  private lazy val toPathStructure: String => PathStructure = fullPath =>
    if(fullPath.isEmpty) PathStructure("", Map.empty[String, String])
    else {
      fullPath.split('?').toList match {
        case Nil => PathStructure("", Map.empty[String, String]) //should never happen
        case x :: Nil => PathStructure(x, Map.empty[String, String])
        case x :: xs =>

          val params: Map[String, String] = Helpers.pair(xs.mkString.split('&').toList.flatMap(p => p.split('=').toList))

          PathStructure(x, params)
      }
    }
}

case class PathStructure(path: String, params: Map[String, String])
