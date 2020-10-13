package com.itv.scalapactcore.verifier

import java.net.URLEncoder

import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter, IScalaPactHttpClient}
import com.itv.scalapact.shared.{Helpers, HttpMethod, Pact, PactBrokerAuthorization, PactLogger, PactVerifySettings, PactsForVerificationRequest, ScalaPactSettings, SimpleRequest, SimpleResponse, SslContextMap, VersionedConsumer}
import com.itv.scalapact.shared.ColourOutput._
import com.itv.scalapactcore.common.PactBrokerAddressValidation
import com.itv.scalapactcore.verifier.PactBrokerHelpers._

import scala.concurrent.duration.Duration
import scala.util.Left

private[verifier] class PactBrokerClient[F[_]](implicit pactReader: IPactReader,
                              pactWriter: IPactWriter,
                              sslContextMap: SslContextMap,
                              httpClient: IScalaPactHttpClient[F]) {

  def fetchPacts(pactVerifySettings: PactVerifySettings,
                 arguments: ScalaPactSettings): List[Pact] = {
    if (pactVerifySettings.consumerVersionSelectors.nonEmpty) {
      providerPactsForVerificationUrl(pactVerifySettings, arguments) match {
        case Some(url) =>
          fetchFromPactsForVerification(url, pactVerifySettings, arguments)
        case None =>
          //TODO this case needs to be handled more gracefully if possible.
          PactLogger.warn(s"pb:provider-pacts-for-verification relation unavailable".yellow.bold)
          Nil
      }
    } else prePactsForVerificationEndpointFetch(pactVerifySettings, arguments)
  }

  private def providerPactsForVerificationUrl(pactVerifySettings: PactVerifySettings, arguments: ScalaPactSettings): Option[String] = {
    pactVerifySettings.consumerVersionSelectors.headOption.flatMap { _ =>
      val request = SimpleRequest(
        baseUrl = arguments.giveHost,
        endPoint = "",
        method = HttpMethod.GET,
        headers = Map("Accept" -> "application/hal+json"),
        body = None,
        sslContextName = None
      )
      httpClient.doRequestSync(request, arguments.giveClientTimeout) match {
        case Right(resp) if resp.is2xx => resp.body.map(pactReader.jsonStringToHALIndex).flatMap {
          case Right(index) => index._links.get("pb:provider-pacts-for-verification").map(_.href)
          case Left(_) =>
            PactLogger.error("HAL index missing from Pact Broker response")
            throw new Exception("HAL index missing from Pact Broker response")
        }
        case Right(_) =>
          PactLogger.error(s"Failed to load HAL index from: ${arguments.giveHost}".red)
          throw new Exception(s"Failed to load HAL index from: ${arguments.giveHost}")
        case Left(e) =>
          PactLogger.error(s"Error: ${e.getMessage}".red)
          throw e
      }
    }
  }

  private def fetchFromPactsForVerification(address: String, pactVerifySettings: PactVerifySettings, arguments: ScalaPactSettings): List[Pact] = {
    val body = pactWriter.pactsForVerificationRequestToJsonString(
      PactsForVerificationRequest(pactVerifySettings.consumerVersionSelectors, pactVerifySettings.providerVersionTags)
    )
    val request = SimpleRequest(
      baseUrl = address,
      endPoint = "",
      method = HttpMethod.POST,
      headers = Map("Accept" -> "application/hal+json", "Content-Type" -> "application/json"),
      body = Some(body),
      sslContextName = None
    )

    httpClient.doRequestSync(request, arguments.giveClientTimeout) match {
      case Right(resp) if resp.is2xx => resp.body.map(pactReader.jsonStringToPactsForVerification).map {
        case Right(pactsForVerification) =>
          pactsForVerification.pacts.map(_.href).flatMap {
            case Some(href) => List(
              fetchAndReadPact(href, pactVerifySettings.pactBrokerAuthorization, arguments.giveClientTimeout).getOrThrow
            )
            case None => Nil //This shouldn't happen
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
  }

  private def prePactsForVerificationEndpointFetch(pactVerifySettings: PactVerifySettings, arguments: ScalaPactSettings): List[Pact] = {
    val versionConsumers = pactVerifySettings.consumerNames.map(c => VersionedConsumer(c, "/latest")) ++
      pactVerifySettings.versionedConsumerNames.map(vc => vc.copy(version = "/version/" + vc.version)) ++
      pactVerifySettings.taggedConsumerNames.flatMap(
        tc => tc.tags.map(t => VersionedConsumer(tc.name, "/latest/" + URLEncoder.encode(t, "UTF-8")))
      )

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
              fetchAndReadPact(
                validatedAddress.address + "/pacts/provider/" + providerName + "/consumer/" + consumerName + consumer.version,
                pactVerifySettings.pactBrokerAuthorization,
                arguments.giveClientTimeout
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

  private def fetchAndReadPact(
                                address: String,
                                pactBrokerAuthorization: Option[PactBrokerAuthorization],
                                clientTimeout: Duration,
                              ): Either[Throwable, Pact] = {
    PactLogger.message(s"Attempting to fetch pact from pact broker at: $address".white.bold)

    httpClient
      .doRequestSync(
        SimpleRequest(address,
          "",
          HttpMethod.GET,
          Map("Accept" -> "application/json") ++ pactBrokerAuthorization.map(_.asHeader).toList,
          None,
          sslContextName = None),
        clientTimeout
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

}

object PactBrokerHelpers {
  implicit class EitherOps[A](val e: Either[Throwable, A]) extends AnyVal {
    def getOrThrow: A = e match {
      case Right(a) => a
      case Left(e) => throw e
    }
  }
}
