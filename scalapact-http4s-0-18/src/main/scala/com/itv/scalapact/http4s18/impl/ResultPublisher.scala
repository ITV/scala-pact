package com.itv.scalapact.http4s18.impl
import cats.effect.IO
import cats.implicits._
import com.itv.scalapact.shared.{IResultPublisher, _}
import org.http4s.client.Client
import com.itv.scalapact.shared.ColourOuput._

import scala.concurrent.duration._

class ResultPublisher(fetcher: (SimpleRequest, IO[Client[IO]]) => IO[SimpleResponse]) extends IResultPublisher {

  val maxTotalConnections: Int = 2

  override def publishResults(pactVerifyResults: List[PactVerifyResult], brokerPublishData: BrokerPublishData)(implicit sslContextMap: SslContextMap): Unit = {
    pactVerifyResults
      .map { result =>
      result.pact._links.flatMap(_.get("pb:publish-verification-results")).map(_.href) match {
        case Some(link) =>
          val success = !result.results.exists(_.result.isLeft)
          val body = s"""{"success": "$success", "providerApplicationVersion": "${brokerPublishData.providerVersion}"}"""
          val request = SimpleRequest(link, "", HttpMethod.POST, Map("Content-Type" -> "application/json; charset=UTF-8"), Option(body), None)

          SslContextMap(request)(
            sslContext =>
              simpleRequestWithoutFakeHeader => {
                val client = Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, 2.seconds, sslContext)
                fetcher(simpleRequestWithoutFakeHeader, client)
                  .map { response =>
                    if (response.is2xx) {
                      PactLogger.message(
                          s"Verification results published for provider ${result.pact.provider} and consumer ${result.pact.consumer}"
                        )
                    } else {
                      PactLogger.error(s"Publish verification results failed with ${response.statusCode}".red)
                    }
                  }
              }
          )
        case None =>
          IO.pure(
              PactLogger
                .error("Unable to publish verification results as there is no pb:publish-verification-results link".red)
            )
      }
    }
      .sequence
      .map(_ => ())
      .unsafeRunSync()
  }
}
