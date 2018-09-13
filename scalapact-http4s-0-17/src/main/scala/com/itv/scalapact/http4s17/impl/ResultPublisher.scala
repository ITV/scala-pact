package com.itv.scalapact.http4s17.impl

import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared._
import fs2.Task
import org.http4s.client.Client

import scala.concurrent.duration._

class ResultPublisher(fetcher: (SimpleRequest, Client) => Task[SimpleResponse]) extends IResultPublisher {

  val maxTotalConnections: Int = 2

  override def publishResults(pactVerifyResults: List[PactVerifyResult], brokerPublishData: BrokerPublishData)(implicit sslContextMap: SslContextMap): Unit = {
    Task.traverse(pactVerifyResults){ result =>
      result.pact._links.flatMap(_.get("pb:publish-verification-results")).map(_.href) match {
        case Some(link) =>
          val success = !result.results.exists(_.result.isLeft)
          val request = SimpleRequest(link, "", HttpMethod.POST, Map("Content-Type" -> "application/json; charset=UTF-8"), body(brokerPublishData, success), None)

          SslContextMap(request)(
            sslContext =>
              simpleRequestWithoutFakeHeader => {
                val client = Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, 2.seconds, sslContext)
                fetcher(simpleRequestWithoutFakeHeader, client)
                  .map { response =>
                    if (response.is2xx) {
                      PactLogger.message(s"Verification results published for provider ${result.pact.provider} and consumer ${result.pact.consumer}")
                    } else {
                      PactLogger.error(s"Publish verification results failed with ${response.statusCode}".red)
                    }
                  }
              }
          )
        case None =>
          Task.now(PactLogger.error("Unable to publish verification results as there is no pb:publish-verification-results link".red))
      }
    }
  }.map(_ => ()).unsafeRun()

  private def body(brokerPublishData: BrokerPublishData, success: Boolean) = {
    val buildUrl = brokerPublishData.buildUrl.fold("")(u => s""", "buildUrl": "$u" """)
    Option(s"""{"success": "$success", "providerApplicationVersion": "${brokerPublishData.providerVersion}"$buildUrl}""")
  }
}
