package com.itv.scalapact.plugin.stubber

import com.itv.scalapactcore.Interaction

import scalaz._
import Scalaz._

object InteractionManager extends InteractionManager

//Use trait for testing or you'll have race conditions!
trait InteractionManager {

  private var interactions = List.empty[Interaction]

  private val matchHeaders: Option[Map[String, String]] => Option[Map[String, String]] => Boolean = expected => received =>
    (expected |@| received) { (e, r) => e.toSet.subsetOf(r.toSet) } match {
      case Some(s) => s
      case None => true
    }

  private val toPathStructure: String => PathStructure = fullPath => {
    if(fullPath.isEmpty) PathStructure("", Map.empty[String, String])
    else {
      fullPath.split('?').toList match {
        case Nil => PathStructure("", Map.empty[String, String]) //should never happen
        case x :: Nil => PathStructure(x, Map.empty[String, String])
        case x :: xs =>

          val params: Map[String, String] = Convertors.pair(xs.mkString.split('&').toList.flatMap(p => p.split('=').toList))

          PathStructure(x, params)
      }
    }
  }

  private val matchPaths: Option[String] => Option[String] => Boolean = expected => received =>
    (expected |@| received) { (e, r) =>
      toPathStructure(e) == toPathStructure(r)
    } match {
      case Some(s) => s
      case None => true
    }

  def findMatchingInteraction(request: RequestDetails): Option[Interaction] = {
    interactions.find{ i =>
      i.request.method == request.method &&
        matchHeaders(i.request.headers)(request.headers) &&
        matchPaths(i.request.path)(request.path) &&
        i.request.body == request.body
    }
  }

  def getInteractions: List[Interaction] = interactions

  def addInteraction(interaction: Interaction): Unit = interactions = interaction :: interactions

  def addInteractions(interactions: List[Interaction]): Unit = interactions.foreach(addInteraction)

  def clearInteractions(): Unit = interactions = List.empty[Interaction]

}

case class PathStructure(path: String, params: Map[String, String])

case class RequestDetails(method: Option[String], headers: Option[Map[String, String]], path: Option[String], body: Option[String])