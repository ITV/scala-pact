package com.itv.scalapact

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets

import com.itv.scalapact.model.ScalaPactMatchingRule.{ScalaPactMatchingRuleArrayMinLength, ScalaPactMatchingRuleRegex, ScalaPactMatchingRuleType}
import com.itv.scalapact.model.{ScalaPactDescriptionFinal, ScalaPactInteractionFinal, ScalaPactMatchingRule}
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.json.IPactWriter

private[scalapact] object ScalaPactContractWriter {
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

  private implicit class MapOps(val m: Map[String, String]) extends AnyVal {
    def toOption: Option[Map[String, String]] = if (m.nonEmpty) Some(m) else None
  }

  private implicit class StringOps(val s: String) extends AnyVal {
    def toOption: Option[String] = if (s.nonEmpty) Some(s) else None
  }

  private implicit class IntOps(val i: Int) extends AnyVal {
    def positive: Option[Int] = if (i > 0) Some(i) else None
  }

  private val simplifyName: String => String = name => "[^a-zA-Z0-9-]".r.replaceAllIn(name.replace(" ", "-"), "")

  private def producePactJson(pactDescription: ScalaPactDescriptionFinal)(implicit pactWriter: IPactWriter): String =
    pactWriter.pactToJsonString(
      producePactFromDescription(pactDescription),
      BuildInfo.version
    )

  def producePactFromDescription: ScalaPactDescriptionFinal => Pact =
    pactDescription =>
      shared.Pact(
        provider = PactActor(pactDescription.provider),
        consumer = PactActor(pactDescription.consumer),
        interactions = pactDescription.interactions.map(convertInteractionsFinalToInteractions),
        _links = None,
        metadata = Option(
          PactMetaData(
            pactSpecification = Option(VersionMetaData("2.0.0")), // TODO: Where should this value come from?
            `scala-pact` = Option(VersionMetaData(BuildInfo.version))
          )
        )
    )

  private def convertInteractionsFinalToInteractions: ScalaPactInteractionFinal => Interaction = i => {
    val pathAndQuery: (String, String) = i.request.path.split('?').toList ++ List(i.request.query.getOrElse("")) match {
      case Nil     => ("/", "")
      case x :: xs => (x, xs.filter(!_.isEmpty).mkString("&"))
    }

    Interaction(
      providerState = i.providerState,
      description = i.description,
      request = InteractionRequest(
        method = i.request.method.name.toOption,
        path = pathAndQuery._1.toOption,
        query = pathAndQuery._2.toOption,
        headers = i.request.headers.toOption,
        body = i.request.body,
        matchingRules = convertMatchingRules(i.request.matchingRules)
      ),
      response = InteractionResponse(
        status = i.response.status.positive,
        headers = i.response.headers.toOption,
        body = i.response.body,
        matchingRules = convertMatchingRules(i.response.matchingRules)
      )
    )
  }

  private def convertMatchingRules(
      rules: Option[List[ScalaPactMatchingRule]]
  ): Option[Map[String, MatchingRule]] =
    rules.map { rs =>
      rs.foldLeft(Map.empty[String, MatchingRule]) {
          case (mrs, ScalaPactMatchingRuleType(key)) =>
            mrs + (key -> MatchingRule(Some("type"), None, None))

          case (mrs, ScalaPactMatchingRuleRegex(key, regex)) =>
            mrs + (key -> MatchingRule(Some("regex"), regex.toOption, None))

          case (mrs, ScalaPactMatchingRuleArrayMinLength(key, min)) =>
            mrs + (key -> MatchingRule(Some("type"), None, min.positive))

        }
    }
}
