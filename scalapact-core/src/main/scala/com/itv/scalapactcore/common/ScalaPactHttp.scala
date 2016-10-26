package com.itv.scalapactcore.common

import java.util.concurrent.ExecutorService

import com.itv.scalapactcore.{InteractionRequest, InteractionResponse}
import org.http4s.client.Client
import org.http4s.client.blaze.{BlazeClientConfig, PooledHttp1Client}
import org.http4s.headers.{AgentProduct, `User-Agent`}
import org.http4s.{BuildInfo, Method, Response, Uri, _}

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}
import scalaz.concurrent.Task

object ScalaPactHttp {

  private val client = HttpClientHelper.buildPooledBlazeHttpClient(1, None)

  def doRequest(method: HttpMethod, baseUrl: String, endPoint: String, headers: Map[String, String], body: Option[String]): Task[SimpleResponse] = {
    HttpClientHelper.doRequest(baseUrl, endPoint, method.http4sMethod, headers, body, client)
  }

  def doInteractionRequest(url: String, ir: InteractionRequest): Task[InteractionResponse] = {
    HttpClientHelper.doRequest(
      baseUrl = url,
      endPoint = ir.path.getOrElse("") + ir.query.map(s => "?" + s).getOrElse(""),
      method = maybeMethodToMethod(ir.method).http4sMethod,
      headers = ir.headers.getOrElse(Map.empty[String, String]),
      body = ir.body,
      httpClient = client
    ).map { r =>
      InteractionResponse(
        status = Option(r.statusCode),
        headers = if(r.headers.isEmpty) None else Option(r.headers.map(p => p._1 -> p._2.mkString)),
        body = r.body,
        matchingRules = None
      )
    }
  }

  private val maybeMethodToMethod: Option[String] => HttpMethod = maybeMethod =>
    maybeMethod.map {
      case "GET" => GET
      case "POST" => POST
      case "PUT" => PUT
      case "DELETE" => DELETE
      case "OPTIONS" => OPTIONS
    }.getOrElse(GET)

  sealed trait HttpMethod {
    val http4sMethod: Method
  }
  case object GET extends HttpMethod {
    val http4sMethod: Method = Method.GET
  }
  case object POST extends HttpMethod {
    val http4sMethod: Method = Method.POST
  }
  case object PUT extends HttpMethod {
    val http4sMethod: Method = Method.PUT
  }
  case object DELETE extends HttpMethod {
    val http4sMethod: Method = Method.DELETE
  }
  case object OPTIONS extends HttpMethod {
    val http4sMethod: Method = Method.OPTIONS
  }

}

final case class SimpleResponse(statusCode: Int, headers: Map[String, String], body: Option[String]) {
  def is2xx: Boolean = statusCode >= 200 && statusCode < 300

  def is3xx: Boolean = statusCode >= 300 && statusCode < 400

  def is4xx: Boolean = statusCode >= 400 && statusCode < 500

  def is5xx: Boolean = statusCode >= 500 && statusCode < 600
}

object HttpClientHelper {

  private def headersToMap(headers: Headers): Map[String, String] =
    headers.toList
      .map(h => Header.unapply(h))
      .collect { case Some(h) => (h._1.toString, h._2) }
      .toMap

  private lazy val blazeClientConfig: Option[ExecutorService] => BlazeClientConfig = maybeExecutor => BlazeClientConfig.defaultConfig.copy(
    requestTimeout = 1.second,
    userAgent = Option(`User-Agent`(AgentProduct("thor", Option(BuildInfo.version)))),
    endpointAuthentication = false,
    customExecutor = maybeExecutor
  )

  def buildPooledBlazeHttpClient(maxTotalConnections: Int, maybeExecutor: Option[ExecutorService]): Client =
    PooledHttp1Client(maxTotalConnections, blazeClientConfig(maybeExecutor))

  private def buildUri(baseUrl: String, endpoint: String): Task[Uri] =
    Task.fromDisjunction(
      Uri.fromString(baseUrl + endpoint).leftMap(l => new Exception(l.message))
    )

  private lazy val extractResponse: Response => Task[SimpleResponse] = response =>
    response.bodyAsText.runLog[Task, String].map { maybeBody =>
      SimpleResponse(response.status.code, headersToMap(response.headers), Some(maybeBody.mkString))
    }

  def doRequest(baseUrl: String, endPoint: String, method: Method, headers: Map[String, String], body: Option[String], httpClient: Client): Task[SimpleResponse] =
    for {
      uri <- buildUri(baseUrl, endPoint)
      request <- Http4sRequestResponseFactory.buildRequest(method, uri, headers, body)
      response <- httpClient.fetch[SimpleResponse](request)(extractResponse)
    } yield response

}
