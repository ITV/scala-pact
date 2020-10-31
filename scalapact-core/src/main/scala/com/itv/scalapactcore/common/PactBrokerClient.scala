package com.itv.scalapactcore.common

import java.net.URLEncoder

import com.itv.scalapact.shared
import com.itv.scalapact.shared.utils.ColourOutput._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.http.{HttpMethod, IScalaPactHttpClient, IScalaPactHttpClientBuilder, SimpleRequest, SimpleResponse}
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.utils.{Helpers, PactLogger}
import com.itv.scalapactcore.common.PactBrokerHelpers._
import com.itv.scalapactcore.publisher.{PublishFailed, PublishResult, PublishSuccess}

import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Left

class PactBrokerClient(
  implicit pactReader: IPactReader,
  pactWriter: IPactWriter,
  httpClientBuilder: IScalaPactHttpClientBuilder) {

  def fetchPactsFromPactsForVerification(pactVerifySettings: PactsForVerificationSettings): List[(Pact, VerificationProperties)] = {
    val brokerClient = httpClientBuilder.buildWithDefaults(pactVerifySettings.pactBrokerClientTimeout, pactVerifySettings.sslContextName)

    providerPactsForVerificationUrl(brokerClient)(pactVerifySettings).map { address =>
      val body = pactWriter.pactsForVerificationRequestToJsonString(
        shared.PactsForVerificationRequest(pactVerifySettings.consumerVersionSelectors, pactVerifySettings.providerVersionTags, pactVerifySettings.includePendingStatus)
      )
      val request = SimpleRequest(
        baseUrl = address,
        endPoint = "",
        method = HttpMethod.POST,
        headers = Map("Accept" -> "application/hal+json", "Content-Type" -> "application/json") ++ pactVerifySettings.pactBrokerAuthorization.map(_.asHeader).toList,
        body = Some(body),
        sslContextName = None
      )

      brokerClient.doRequest(request) match {
        case Right(resp) if resp.is2xx => resp.body.map(pactReader.jsonStringToPactsForVerification).map {
          case Right(pactsForVerification) =>
            pactsForVerification.pacts.flatMap {
              case e@PactForVerification(props, _) =>
                e.href.map { link =>
                  List((fetchAndReadPact(brokerClient)(link, pactVerifySettings.pactBrokerAuthorization).getOrThrow, props))
                }.getOrElse(Nil) //This shouldn't happen
            }
          case Left(e) =>
            PactLogger.error(e.red)
            Nil
        }.getOrElse {
          PactLogger.error("Pact data missing from Pact Broker response")
          throw new Exception("Pact data missing from Pact Broker response")
        }
        case Right(_) =>
          PactLogger.error(s"Failed to load pacts for verification from: $address".red)
          throw new Exception(s"Failed to load pacts for verification from: $address")
        case Left(e) =>
          PactLogger.error(s"Error: ${e.getMessage}".red)
          throw e
      }
    }.getOrElse {
      PactLogger.warn(s"pb:provider-pacts-for-verification relation unavailable".yellow.bold)
      Nil
    }
  }

  private def providerPactsForVerificationUrl(brokerClient: IScalaPactHttpClient)(pactVerifySettings: PactsForVerificationSettings): Option[String] = {
    pactVerifySettings.consumerVersionSelectors.headOption.flatMap { _ =>
      val request = SimpleRequest(
        baseUrl = pactVerifySettings.pactBrokerAddress,
        endPoint = "",
        method = HttpMethod.GET,
        headers = Map("Accept" -> "application/hal+json") ++ pactVerifySettings.pactBrokerAuthorization.map(_.asHeader).toList,
        body = None,
        sslContextName = None
      )
      PactLogger.message("Attempting to fetch relation 'pb:provider-pacts-for-verification' from broker".black)
      val templateUrl = brokerClient.doRequest(request) match {
        case Right(resp) if resp.is2xx => resp.body.map(pactReader.jsonStringToHALIndex).flatMap {
          case Right(index) => index._links.get("pb:provider-pacts-for-verification").map(_.href)
          case Left(_) =>
            PactLogger.error(s"HAL index missing from Pact Broker response".red)
            throw new Exception("HAL index missing from Pact Broker response")
        }
        case Right(_) =>
          PactLogger.error(s"Failed to load HAL index from: ${pactVerifySettings.pactBrokerAddress}".red)
          throw new Exception(s"Failed to load HAL index from: ${pactVerifySettings.pactBrokerAddress}")
        case Left(e) =>
          PactLogger.error(s"Error: ${e.getMessage}".red)
          throw e
      }

      templateUrl.map(_.replace("{provider}", pactVerifySettings.providerName))
    }
  }

  def fetchPactsOldWorld(pactVerifySettings: PrePactsForVerificationSettings): List[Pact] = {
    val brokerClient = httpClientBuilder.buildWithDefaults(pactVerifySettings.pactBrokerClientTimeout, pactVerifySettings.sslContextName)

    val versionConsumers = pactVerifySettings match {
      case LatestConsumerVerifySettings(_, _, _, consumerNames, _, _, _) =>
        consumerNames.map(VersionedConsumer.fromName)
      case TaggedConsumerVerifySettings(_, _, _, taggedConsumerNames, _, _, _) =>
        taggedConsumerNames.flatMap(
          tc => VersionedConsumer.fromNameAndTags(tc.name, tc.tags)
        )
      case VersionedConsumerVerifySettings(_, _, _, versionedConsumerNames, _, _, _) =>
        versionedConsumerNames
    }

    val maybePacts = for {
      providerName <- Helpers.urlEncode(pactVerifySettings.providerName)
      validatedAddress <- PactBrokerAddressValidation.checkPactBrokerAddress(pactVerifySettings.pactBrokerAddress)
    } yield
      versionConsumers.flatMap { consumer =>
        Helpers.urlEncode(consumer.name) match {
          case Left(l) =>
            PactLogger.error(l.red)
            Nil
          case Right(consumerName) =>
            List(
              fetchAndReadPact(brokerClient)(
                validatedAddress.address + "/pacts/provider/" + providerName + "/consumer/" + consumerName + consumer.versionUrlPart.value,
                pactVerifySettings.pactBrokerAuthorization
              ).getOrThrow
            )
        }
      }

    maybePacts match {
      case Left(l) =>
        PactLogger.error(l.red)
        Nil
      case Right(pacts) => pacts
    }
  }

  private def fetchAndReadPact(brokerClient: IScalaPactHttpClient)(
    address: String,
    pactBrokerAuthorization: Option[PactBrokerAuthorization]
  ): Either[Throwable, Pact] = {
    PactLogger.message(s"Attempting to fetch pact from pact broker at: $address".white.bold)

    brokerClient
      .doRequest(
        SimpleRequest(address,
          "",
          HttpMethod.GET,
          Map("Accept" -> "application/json") ++ pactBrokerAuthorization.map(_.asHeader).toList,
          None,
          sslContextName = None)
      ) match {
      case Right(r: SimpleResponse) if r.is2xx =>
        r.body
          .map(pactReader.jsonStringToPact)
          .map {
            case Right(p) =>
              Right(p)
            case Left(msg) =>
              PactLogger.error(s"Error: $msg".yellow)
              PactLogger.error("Could not convert good response to Pact:\n" + r.body.getOrElse(""))
              Left(new Exception(s"Failed to load consumer pact from: $address"))
          }
          .getOrElse {
            PactLogger.error("Pact data missing from Pact Broker response")
            Left(new Exception("Pact data missing from Pact Broker response"))
          }

      case Right(_) =>
        PactLogger.error(s"Failed to load consumer pact from: $address".red)
        Left(new Exception(s"Failed to load consumer pact from: $address"))

      case Left(e) =>
        PactLogger.error(s"Error: ${e.getMessage}".red)
        Left(e)
    }

  }

  def publishVerificationResults(
                      pactVerifyResults: List[PactVerifyResult],
                      brokerPublishData: BrokerPublishData,
                      pactBrokerAuthorization: Option[PactBrokerAuthorization],
                      brokerClientTimeout: Option[Duration],
                      sslContextName: Option[String]
                    ): Unit = {
    val httpClient = httpClientBuilder.build(brokerClientTimeout.getOrElse(2.seconds), sslContextName, 2)
    pactVerifyResults.foreach { result =>
      result.pact._links.flatMap(_.get("pb:publish-verification-results")).map(_.href) match {
        case Some(link) =>
          val success = !result.results.exists(_.result.isLeft)
          val request = SimpleRequest(
            link,
            "",
            HttpMethod.POST,
            Map("Content-Type" -> "application/json; charset=UTF-8") ++ pactBrokerAuthorization.map(_.asHeader).toList,
            body(brokerPublishData, success),
            None
          )
          httpClient.doRequest(request) match {
            case Right(response) =>
              if (response.is2xx) {
                PactLogger.message(
                  s"Verification results published for provider '${result.pact.provider.name}' and consumer '${result.pact.consumer.name}'"
                )
              } else {
                PactLogger.error(s"Publish verification results failed with ${response.statusCode}".red)
              }
            case Left(err) => PactLogger.error(s"Unable to publish verification results: $err".red)
          }
        case None =>
          PactLogger.error("Unable to publish verification results as there is no pb:publish-verification-results link".red)
      }
    }
  }

  private def body(brokerPublishData: BrokerPublishData, success: Boolean): Option[String] = {
    val buildUrl = brokerPublishData.buildUrl.fold("")(u => s""", "buildUrl": "$u"""")
    Some(s"""{ "success": $success, "providerApplicationVersion": "${brokerPublishData.providerVersion}"$buildUrl }""")
  }

  def publishPacts(pacts: List[Pact], settings: PactPublishSettings): List[PublishResult] = {
    val client = httpClientBuilder.build(settings.pactBrokerClientTimeout, settings.sslContextName, 2)
    publishToBroker(client, pacts, settings, settings.pactBrokerAddress) ++ publishToOtherBrokers(client, pacts, settings)
  }

  private def publishToBroker(client: IScalaPactHttpClient, pacts: List[Pact], settings: PactPublishSettings, brokerAddress: String): List[PublishResult] =
    PactBrokerAddressValidation.checkPactBrokerAddress(brokerAddress) match {
      case Left(err) => List(PublishFailed("Validation error", err))
      case Right(address) =>
        val version = settings.projectVersion.replace("-SNAPSHOT", ".x")
        pacts.map(publishSinglePact(client, address, version, settings.tagsToPublishWith, settings.sslContextName, settings.pactBrokerAuthorization))
    }

  private def publishToOtherBrokers(client: IScalaPactHttpClient, pacts: List[Pact], settings: PactPublishSettings): List[PublishResult] =
    pacts.map(_.provider.name)
      .flatMap(name => settings.providerBrokerPublishMap.get(name).toList)
      .flatMap(publishToBroker(client, pacts, settings, _))

  private def publishSinglePact(
                                 httpClient: IScalaPactHttpClient,
                                 pactBrokerAddress: ValidPactBrokerAddress,
                                 version: String,
                                 tagsToPublishWith: List[String],
                                 sslContextName: Option[String],
                                 pactBrokerAuthorization: Option[PactBrokerAuthorization])(pact: Pact): PublishResult = {
    val names = for {
      consumer <- Helpers.urlEncode(pact.consumer.name)
      provider <- Helpers.urlEncode(pact.provider.name)
    } yield (consumer, provider)
    names match {
      case Left(err) => PublishFailed("Validation error", err)
      case Right((consumer, provider)) =>
        val tagAddresses = tagsToPublishWith.map { t =>
          pactBrokerAddress.address + "/pacticipants/" + consumer + "/versions/" + version + "/tags/" + URLEncoder
            .encode(t, "UTF-8")
        }
        val address = pactBrokerAddress.address + "/pacts/provider/" + provider + "/consumer/" + consumer + "/version/" + version
        val tagContext = if (tagAddresses.nonEmpty) s" (With tags: ${tagsToPublishWith.mkString(", ")})" else ""
        val context    = s"Publishing '$consumer -> $provider'$tagContext to:\n > $address".yellow
        val tagResponses = tagAddresses.map { tagAddress =>
          httpClient.doRequest(
            SimpleRequest(tagAddress,
              "",
              HttpMethod.PUT,
              Map("Content-Type" -> "application/json", "Content-Length" -> "0") ++ pactBrokerAuthorization.map(_.asHeader).toList,
              Option(pactWriter.pactToJsonString(pact, BuildInfo.version)),
              sslContextName = sslContextName)
          )
        }

        tagResponses
          .collectFirst[PublishResult] {
            case Left(e) =>
              PublishFailed(context, s"Failed with error: ${e.getMessage}".red)
            case Right(r) if !r.is2xx =>
              PublishFailed(context, s"$r\nFailed: ${r.statusCode}, ${r.body}".red)
          }
          .getOrElse {
            httpClient.doRequest(
              SimpleRequest(address,
                "",
                HttpMethod.PUT,
                Map("Content-Type" -> "application/json") ++ pactBrokerAuthorization.map(_.asHeader).toList,
                Option(pactWriter.pactToJsonString(pact, BuildInfo.version)),
                sslContextName = sslContextName)
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

}

object PactBrokerHelpers {
  implicit class EitherOps[A](val e: Either[Throwable, A]) extends AnyVal {
    def getOrThrow: A = e match {
      case Right(a) => a
      case Left(e) => throw e
    }
  }
}
