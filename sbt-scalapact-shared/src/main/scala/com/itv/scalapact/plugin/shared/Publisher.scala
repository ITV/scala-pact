package com.itv.scalapact.plugin.shared

import java.net.URLEncoder

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.ColourOutput._
import com.itv.scalapact.shared.typeclasses.IPactWriter
import com.itv.scalapactcore.verifier.ValidatedDetails

object Publisher {

  def publishToBroker(
      sendIt: SimpleRequest => Either[Throwable, SimpleResponse],
      pactBrokerAddress: String,
      versionToPublishAs: String,
      tagsToPublishWith: Seq[String],
      pactBrokerCredentials: Option[BasicAuthenticationCredentials]
  )(implicit pactWriter: IPactWriter): ConfigAndPacts => List[PublishResult] =
    configAndPacts =>
      configAndPacts.pacts.map { pact =>
        publishPact(sendIt, pact, versionToPublishAs, tagsToPublishWith, pactBrokerCredentials) {
          ValidatedDetails.buildFrom(pact.consumer.name, pact.provider.name, pactBrokerAddress, "/latest")
        }
    }

  def publishPact(sendIt: SimpleRequest => Either[Throwable, SimpleResponse],
                  pact: Pact,
                  versionToPublishAs: String,
                  tagsToPublishWith: Seq[String],
                  basicAuthCredentials: Option[BasicAuthenticationCredentials])(
      details: Either[String, ValidatedDetails]
  )(implicit pactWriter: IPactWriter): PublishResult =
    details match {
      case Left(l) =>
        PublishFailed("Validation error", l)

      case Right(v) =>
        val consumerVersion = versionToPublishAs.replace("-SNAPSHOT", ".x")
        val tagAddresses = tagsToPublishWith.map(
          v.validatedAddress.address + "/pacticipants/" + v.consumerName + "/versions/" + consumerVersion + "/tags/" + URLEncoder
            .encode(_, "UTF-8")
        )
        val address = v.validatedAddress.address + "/pacts/provider/" + v.providerName + "/consumer/" + v.consumerName + "/version/" + consumerVersion

        val tagContext = if (tagAddresses.nonEmpty) s" (With tags: ${tagsToPublishWith.mkString(", ")})" else ""
        val context    = s"Publishing '${v.consumerName} -> ${v.providerName}'$tagContext to:\n > $address".yellow

        val tagResponses = tagAddresses.map(tagAddress => {
          sendIt(
            SimpleRequest(tagAddress,
                          "",
                          HttpMethod.PUT,
                          Map("Content-Type" -> "application/json", "Content-Length" -> "0") ++ basicAuthCredentials.map(_.asHeader).toList,
                          None,
                          sslContextName = None)
          )
        })

        tagResponses
          .collectFirst[PublishResult] {
            case Left(e) =>
              PublishFailed(context, s"Failed with error: ${e.getMessage}".red)
            case Right(r) if !r.is2xx =>
              PublishFailed(context, s"$r\nFailed: ${r.statusCode}, ${r.body}".red)
          }
          .getOrElse {
            sendIt(
              SimpleRequest(address,
                            "",
                            HttpMethod.PUT,
                            Map("Content-Type" -> "application/json") ++ basicAuthCredentials.map(_.asHeader).toList,
                            Option(pactWriter.pactToJsonString(pact)),
                            sslContextName = None)
            ) match {
              case Right(r) if r.is2xx =>
                PublishSuccess(context)

              case Right(r) =>
                PublishFailed(context, s"$r\nFailed: ${r.statusCode}, ${r.body}".red)

              case Left(e) =>
                PublishFailed(context, s"Failed with error: ${e.getMessage}".red)
            }
          }
    }
}

sealed trait PublishResult {
  val renderAsString: String
}
case class PublishSuccess(context: String) extends PublishResult {
  val renderAsString: String =
    s"""${context.yellow}
       |${"Success".green}
     """.stripMargin
}
case class PublishFailed(context: String, message: String) extends PublishResult {
  val renderAsString: String =
    s"""${context.yellow}
       |$message
     """.stripMargin
}
