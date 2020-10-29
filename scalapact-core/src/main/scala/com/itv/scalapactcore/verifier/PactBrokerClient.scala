package com.itv.scalapactcore.verifier

import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter, IScalaPactHttpClient, IScalaPactHttpClientBuilder}
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.ColourOutput._
import com.itv.scalapactcore.common.PactBrokerAddressValidation
import com.itv.scalapactcore.verifier.PactBrokerHelpers._

import scala.util.Left

private[verifier] class PactBrokerClient[F[_]](
  implicit pactReader: IPactReader,
  pactWriter: IPactWriter,
  httpClientBuilder: IScalaPactHttpClientBuilder[F]) {

  def fetchFromPactsForVerification(pactVerifySettings: PactsForVerificationSettings): List[(Pact, VerificationProperties)] = {
    val brokerClient = httpClientBuilder.buildWithDefaults(pactVerifySettings.pactBrokerClientTimeout, pactVerifySettings.sslContextName)

    providerPactsForVerificationUrl(brokerClient)(pactVerifySettings).map { address =>
      val body = pactWriter.pactsForVerificationRequestToJsonString(
        PactsForVerificationRequest(pactVerifySettings.consumerVersionSelectors, pactVerifySettings.providerVersionTags, pactVerifySettings.includePendingStatus)
      )
      val request = SimpleRequest(
        baseUrl = address,
        endPoint = "",
        method = HttpMethod.POST,
        headers = Map("Accept" -> "application/hal+json", "Content-Type" -> "application/json") ++ pactVerifySettings.pactBrokerAuthorization.map(_.asHeader).toList,
        body = Some(body),
        sslContextName = None
      )

      brokerClient.doRequestSync(request) match {
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

  private def providerPactsForVerificationUrl(brokerClient: IScalaPactHttpClient[F])(pactVerifySettings: PactsForVerificationSettings): Option[String] = {
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
      val templateUrl = brokerClient.doRequestSync(request) match {
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

  def prePactsForVerificationEndpointFetch(pactVerifySettings: PrePactsForVerificationSettings): List[Pact] = {
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

  private def fetchAndReadPact(brokerClient: IScalaPactHttpClient[F])(
    address: String,
    pactBrokerAuthorization: Option[PactBrokerAuthorization]
  ): Either[Throwable, Pact] = {
    PactLogger.message(s"Attempting to fetch pact from pact broker at: $address".white.bold)

    brokerClient
      .doRequestSync(
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

}

object PactBrokerHelpers {
  implicit class EitherOps[A](val e: Either[Throwable, A]) extends AnyVal {
    def getOrThrow: A = e match {
      case Right(a) => a
      case Left(e) => throw e
    }
  }
}
