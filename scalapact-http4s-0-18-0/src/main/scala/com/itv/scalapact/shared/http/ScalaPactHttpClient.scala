package com.itv.scalapact.shared.http

import cats.effect.IO
import com.itv.scalapact.shared._
import org.http4s.client.Client

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}

object ScalaPactHttpClient extends IScalaPactHttpClient {

  private val maxTotalConnections: Int = 1

  private implicit val executionContext: ExecutionContext = Http4sClientHelper.executionContext

  def doRequest(simpleRequest: SimpleRequest): Future[SimpleResponse] =
    doRequestIO(Http4sClientHelper.doRequest, simpleRequest).unsafeToFuture()

  def doInteractionRequest(url: String, ir: InteractionRequest, clientTimeout: Duration): Future[InteractionResponse] =
    doInteractionRequestIO(Http4sClientHelper.doRequest, url, ir, clientTimeout).unsafeToFuture()

  def doRequestSync(simpleRequest: SimpleRequest): Either[Throwable, SimpleResponse] =
    doRequestIO(Http4sClientHelper.doRequest, simpleRequest).attempt.unsafeRunSync()

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration): Either[Throwable, InteractionResponse] =
    doInteractionRequestIO(Http4sClientHelper.doRequest, url, ir, clientTimeout).attempt.unsafeRunSync()

  def doRequestIO(performRequest: (SimpleRequest, Client[IO]) => IO[SimpleResponse], simpleRequest: SimpleRequest): IO[SimpleResponse] =
    performRequest(simpleRequest, Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, 2.seconds))

  def doInteractionRequestIO(performRequest: (SimpleRequest, Client[IO]) => IO[SimpleResponse], url: String, ir: InteractionRequest, clientTimeout: Duration): IO[InteractionResponse] =
    performRequest(
      SimpleRequest( url, ir.path.getOrElse("") + ir.query.map(q => s"?$q").getOrElse(""), HttpMethod.maybeMethodToMethod(ir.method), ir.headers.getOrElse(Map.empty[String, String]), ir.body),
      Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, clientTimeout)
    ).map { r =>
      InteractionResponse(
        status = Option(r.statusCode),
        headers = if(r.headers.isEmpty) None else Option(r.headers.map(p => p._1 -> p._2.mkString)),
        body = r.body,
        matchingRules = None
      )
    }

}