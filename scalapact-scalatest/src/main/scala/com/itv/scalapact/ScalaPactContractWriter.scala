package com.itv.scalapact

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets

import com.itv.scalapact.ScalaPactForger.{
  ScalaPactDescriptionFinal,
  ScalaPactInteractionFinal,
  ScalaPactMatchingRule,
  ScalaPactMatchingRuleArrayMinLength,
  ScalaPactMatchingRuleRegex,
  ScalaPactMatchingRuleType
}
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.typeclasses.IPactWriter

import scala.language.implicitConversions

object ScalaPactContractWriter {

  private val simplifyName: String => String = name => "[^a-zA-Z0-9-]".r.replaceAllIn(name.replace(" ", "-"), "")

  def writePactContracts(outputPath: String)(implicit pactWriter: IPactWriter): ScalaPactDescriptionFinal => Unit =
    pactDescription => {
      val dirFile = new File(outputPath)

      if (!dirFile.exists()) {
        dirFile.mkdirs()
      }

      val string = simplifyName(
        pactDescription.consumer + pactDescription.provider + pactDescription.interactions
          .map(_.description)
          .mkString + System.currentTimeMillis().toString
      )

      val sha1 = java.security.MessageDigest
        .getInstance("SHA-1")
        .digest(string.getBytes(StandardCharsets.UTF_8))
        .map("%02x".format(_))
        .mkString

      val relativePath = outputPath + "/" + simplifyName(pactDescription.consumer) + "_" + simplifyName(
        pactDescription.provider
      ) + "_" + sha1 + "_tmp.json"
      val file = new File(relativePath)

      if (file.exists()) {
        file.delete()
      }

      file.createNewFile()

      new PrintWriter(relativePath) {
        write(producePactJson(pactDescription))
        close()
      }

      ()
    }

  private def producePactJson(pactDescription: ScalaPactDescriptionFinal)(implicit pactWriter: IPactWriter): String =
    pactWriter.pactToJsonString(
      producePactFromDescription(pactDescription)
    )

  def producePactFromDescription: ScalaPactDescriptionFinal => Pact =
    pactDescription =>
      Pact(
        provider = PactActor(pactDescription.provider),
        consumer = PactActor(pactDescription.consumer),
        interactions = pactDescription.interactions.map { convertInteractionsFinalToInteractions },
        _links = None
    )

  lazy val convertInteractionsFinalToInteractions: ScalaPactInteractionFinal => Interaction = i => {
    val pathAndQuery: (String, String) = i.request.path.split('?').toList ++ List(i.request.query.getOrElse("")) match {
      case Nil     => ("/", "")
      case x :: xs => (x, xs.filter(!_.isEmpty).mkString("&"))
    }

    Interaction(
      provider_state = None,
      providerState = i.providerState,
      description = i.description,
      request = InteractionRequest(
        method = i.request.method.method,
        path = pathAndQuery._1,
        query = pathAndQuery._2,
        headers = i.request.headers,
        body = i.request.body,
        matchingRules = i.request.matchingRules
      ),
      response = InteractionResponse(
        status = i.response.status,
        headers = i.response.headers,
        body = i.response.body,
        matchingRules = i.response.matchingRules
      )
    )
  }

  implicit private def convertMatchingRules(
      rules: Option[List[ScalaPactMatchingRule]]
  ): Option[Map[String, MatchingRule]] =
    rules.map { rs =>
      rs.map {
          case ScalaPactMatchingRuleType(key) =>
            Map(key -> MatchingRule("type", None, None))

          case ScalaPactMatchingRuleRegex(key, regex) =>
            Map(key -> MatchingRule("regex", regex, None))

          case ScalaPactMatchingRuleArrayMinLength(key, min) =>
            Map(key -> MatchingRule(None, None, min))

        }
        .foldLeft(Map.empty[String, MatchingRule])(_ ++ _)
    }

  implicit private val intToBoolean: Int => Boolean                 = v => v > 0
  implicit private val stringToBoolean: String => Boolean           = v => v != ""
  implicit private val mapToBoolean: Map[String, String] => Boolean = v => v.nonEmpty

  implicit private def valueToOptional[A](value: A)(implicit p: A => Boolean): Option[A] =
    if (p(value)) Option(value) else None

}
