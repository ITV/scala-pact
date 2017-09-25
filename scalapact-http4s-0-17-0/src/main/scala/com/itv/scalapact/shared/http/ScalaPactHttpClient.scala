package com.itv.scalapact.shared.http

import java.util.concurrent.{ExecutorService, Executors}

import com.itv.scalapact.shared.HttpMethod._
import com.itv.scalapact.shared._
import fs2.{Strategy, Task}
import org.http4s.client.Client
import org.http4s.client.blaze.{BlazeClientConfig, PooledHttp1Client}
import org.http4s.headers.{AgentProduct, `User-Agent`}
import org.http4s.{BuildInfo, Method, Response, Uri, _}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}
import RightBiasEither._

object ScalaPactHttpClient extends IScalaPactHttpClient {

  private val nThreads: Int = 50
  private val executorService: ExecutorService = Executors.newFixedThreadPool(nThreads)

  private implicit val strategy: Strategy = Strategy.fromExecutionContext(ExecutionContext.fromExecutorService(executorService))

  def doRequest(method: HttpMethod, baseUrl: String, endPoint: String, headers: Map[String, String], body: Option[String]): Future[SimpleResponse] =
    doRequestTask(method, baseUrl, endPoint, headers, body)

  def doInteractionRequest(url: String, ir: InteractionRequest, clientTimeout: Duration): Future[InteractionResponse] =
    doInteractionRequestTask(url, ir, clientTimeout)

  def doRequestSync(method: HttpMethod, baseUrl: String, endPoint: String, headers: Map[String, String], body: Option[String]): Either[Throwable, SimpleResponse] =
    doRequestTask(method, baseUrl, endPoint, headers, body).unsafeAttemptRun()

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration): Either[Throwable, InteractionResponse] =
    doInteractionRequestTask(url, ir, clientTimeout).unsafeAttemptRun()

  private implicit def taskToFuture[A](x: => Task[A]): Future[A] = {

    val p: Promise[A] = Promise()

    x.unsafeAttemptRun() match {
      case Left(ex) =>
        p.failure(ex)
        ()

      case Right(r) =>
        p.success(r)
        ()
    }

    p.future
  }

  private implicit def httpMethodToMethod(httpMethod: HttpMethod): Method =
    httpMethod match {
      case GET =>
        Method.GET

      case POST =>
        Method.POST

      case PUT =>
        Method.PUT

      case DELETE =>
        Method.DELETE

      case OPTIONS =>
        Method.OPTIONS
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
    checkEndpointIdentification = false
  )

  private def buildUri(baseUrl: String, endpoint: String): Task[Uri] =
    Task.fromAttempt(
      Uri.fromString(baseUrl + endpoint).leftMap(l => new Exception(l.message))
    )

  private def extractResponse(response: Response): Task[SimpleResponse] =
    response.bodyAsText.runLog.map { maybeBody =>
      SimpleResponse(response.status.code, headersToMap(response.headers), Some(maybeBody.mkString))
    }

  def buildPooledBlazeHttpClient(maxTotalConnections: Int, clientTimeout: Duration): Client =
    PooledHttp1Client(maxTotalConnections, blazeClientConfig(clientTimeout))

  def doRequest(baseUrl: String, endPoint: String, method: Method, headers: Map[String, String], body: Option[String], httpClient: Client)(implicit strategy: Strategy): Task[SimpleResponse] =
    for {
      uri <- buildUri(baseUrl, endPoint)
      request <- Http4sRequestResponseFactory.buildRequest(method, uri, headers, body)
      response <- httpClient.fetch(request)(extractResponse)
      _ <- httpClient.shutdown
    } yield response

}
