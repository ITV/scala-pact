package com.itv.plugin.stubber

import com.itv.scalapactcore.Interaction
import org.http4s.Request

object InteractionManager {

  import HeaderImplicitConversions._

  private var interactions = List.empty[Interaction]

  def findMatchingInteraction(request: Request): Option[Interaction] = {

    val method = Option(request.method.name.toUpperCase)
    val headers: Option[Map[String, String]] = Option(request.headers)
    val path = Option(request.pathInfo) //TODO: Not good enough!
    val body = request.bodyAsText.runLast.run

    println("Trying to match: " + method + ", " + path + ", " + headers + ", " + body + ", ")

    interactions.find{ i =>
      i.request.method == method &&
        i.request.headers.toSet.subsetOf(headers.toSet) &&
        i.request.path == path &&
        i.request.body == body
    }
  }

  def getInteractions: List[Interaction] = interactions

  def addInteraction(interaction: Interaction): Unit = interactions = interaction :: interactions

  def addInteractions(interactions: List[Interaction]): Unit = interactions.foreach(addInteraction)

  def clearInteractions(): Unit = interactions = List.empty[Interaction]

}
