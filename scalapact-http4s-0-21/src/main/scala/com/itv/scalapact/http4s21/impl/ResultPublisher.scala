package com.itv.scalapact.http4s21.impl

import cats.effect._
import cats.implicits._
import com.itv.scalapact.shared.ColourOutput._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.typeclasses.{BrokerPublishData, IResultPublisher}
import org.http4s.client.Client

import scala.concurrent.duration.Duration

class ResultPublisher(client: Resource[IO, Client[IO]])(fetcher: (SimpleRequest, Resource[IO, Client[IO]]) => IO[SimpleResponse])
    extends IResultPublisher {

  override def publishResults(
      pactVerifyResults: List[PactVerifyResult],
      brokerPublishData: BrokerPublishData,
      pactBrokerAuthorization: Option[PactBrokerAuthorization]
  ): Unit =
    pactVerifyResults
      .traverse_ { result =>
        result.pact._links.flatMap(_.get("pb:publish-verification-results")).map(_.href) match {
          case Some(link) =>
            val success = !result.results.exists(_.result.isLeft)
            val request = SimpleRequest(
              link,
              "",
              HttpMethod.POST,
              Map("Content-Type" -> "application/json; charset=UTF-8") ++ pactBrokerAuthorization.map(_.asHeader).toList,
              body(brokerPublishData, success).some,
              None
            )
            fetcher(request, client).map { response =>
              if (response.is2xx) {
                PactLogger.message(
                  s"Verification results published for provider ${result.pact.provider} and consumer ${result.pact.consumer}"
                )
              } else {
                PactLogger.error(s"Publish verification results failed with ${response.statusCode}".red)
              }
            }
          case None =>
            IO.pure(
              PactLogger
                .error("Unable to publish verification results as there is no pb:publish-verification-results link".red)
            )
        }
      }
      .unsafeRunSync()

  private def body(brokerPublishData: BrokerPublishData, success: Boolean): String = {
    val buildUrl = brokerPublishData.buildUrl.fold("")(u => s""", "buildUrl": "$u"""")
    s"""{ "success": $success, "providerApplicationVersion": "${brokerPublishData.providerVersion}"$buildUrl }"""
  }
}

object ResultPublisher {
  def apply(clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): IResultPublisher = {
    val client = Http4sClientHelper.buildPooledBlazeHttpClient(2, clientTimeout, sslContextName)
    new ResultPublisher(client)(Http4sClientHelper.doRequest)
  }
}