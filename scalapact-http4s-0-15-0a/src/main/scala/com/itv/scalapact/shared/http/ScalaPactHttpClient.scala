package com.itv.scalapact.shared.http

import com.itv.scalapact.shared.HttpMethod._
import com.itv.scalapact.shared._
import org.http4s.client.Client
import org.http4s.client.blaze.{BlazeClientConfig, PooledHttp1Client}
import org.http4s.headers.{AgentProduct, `User-Agent`}
import org.http4s.{BuildInfo, Method, Response, Uri, _}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}
import scalaz.concurrent.Task

object ScalaPactHttpClient extends IScalaPactHttpClient {

  def doRequest(method: HttpMethod, baseUrl: String, endPoint: String, headers: Map[String, String], body: Option[String]): Future[SimpleResponse] =
    doRequestTask(method, baseUrl, endPoint, headers, body)

  def doInteractionRequest(url: String, ir: InteractionRequest, clientTimeout: Duration): Future[InteractionResponse] =
    doInteractionRequestTask(url, ir, clientTimeout)

  def doRequestSync(method: HttpMethod, baseUrl: String, endPoint: String, headers: Map[String, String], body: Option[String]): Either[Throwable, SimpleResponse] =
    doRequestTask(method, baseUrl, endPoint, headers, body).unsafePerformSyncAttempt.toEither

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration): Either[Throwable, InteractionResponse] =
    doInteractionRequestTask(url, ir, clientTimeout).unsafePerformSyncAttempt.toEither

  private implicit def taskToFuture[A](x: => Task[A]): Future[A] = {
    import scalaz.{ \/-, -\/ }
    val p: Promise[A] = Promise()

    x.unsafePerformAsync {
      case -\/(ex) =>
        p.failure(ex)
        ()

      case \/-(r) =>
        p.success(r)
        ()
    }
    p.future
  }

  private val maxTotalConnections: Int = 1

  private def makeClient: Client = HttpClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, 2.seconds)

  private def doRequestTask(method: HttpMethod, baseUrl: String, endPoint: String, headers: Map[String, String], body: Option[String]): Task[SimpleResponse] =
    HttpClientHelper.doRequest(baseUrl, endPoint, method, headers, body, makeClient)

  private def doInteractionRequestTask(url: String, ir: InteractionRequest, clientTimeout: Duration): Task[InteractionResponse] =
    HttpClientHelper.doRequest(
      baseUrl = url,
      endPoint = ir.path.getOrElse("") + ir.query.map(s => "?" + s).getOrElse(""),
      method = HttpMethod.maybeMethodToMethod(ir.method),
      headers = ir.headers.getOrElse(Map.empty[String, String]),
      body = ir.body,
      httpClient = HttpClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, clientTimeout)
    ).map { r =>
      InteractionResponse(
        status = Option(r.statusCode),
        headers = if(r.headers.isEmpty) None else Option(r.headers.map(p => p._1 -> p._2.mkString)),
        body = r.body,
        matchingRules = None
      )
    }

}

private object HttpClientHelper {

  private def headersToMap(headers: Headers): Map[String, String] =
    headers.toList
      .map(h => Header.unapply(h))
      .collect { case Some(h) => (h._1.toString, h._2) }
      .toMap

  private def blazeClientConfig(clientTimeout: Duration): BlazeClientConfig = BlazeClientConfig.defaultConfig.copy(
    requestTimeout = clientTimeout,
    userAgent = Option(`User-Agent`(AgentProduct("scala-pact", Option(BuildInfo.version)))),
    endpointAuthentication = false,
    customExecutor = None
  )

  private def extractResponse(response: Response): Task[SimpleResponse] =
    response.bodyAsText.runLog[Task, String].map { maybeBody =>
      SimpleResponse(response.status.code, headersToMap(response.headers), Some(maybeBody.mkString))
    }

  def buildPooledBlazeHttpClient(maxTotalConnections: Int, clientTimeout: Duration): Client =
    PooledHttp1Client(maxTotalConnections, blazeClientConfig(clientTimeout))

  def doRequest(baseUrl: String, endPoint: String, method: HttpMethod, headers: Map[String, String], body: Option[String], httpClient: Client): Task[SimpleResponse] =
    for {
      request <- Http4sRequestResponseFactory.buildRequest(method, baseUrl, endPoint, headers, body)
      response <- httpClient.fetch[SimpleResponse](request)(extractResponse)
      _ <- httpClient.shutdown
    } yield response

}
