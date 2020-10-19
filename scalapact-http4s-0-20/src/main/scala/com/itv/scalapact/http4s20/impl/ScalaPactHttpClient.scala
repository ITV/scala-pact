package com.itv.scalapact.http4s20.impl

import cats.effect._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.typeclasses.IScalaPactHttpClient
import org.http4s.client.Client

import scala.concurrent.duration._

class ScalaPactHttpClient(client: Resource[IO, Client[IO]])(fetcher: (SimpleRequest, Resource[IO, Client[IO]]) => IO[SimpleResponse])
  extends IScalaPactHttpClient[IO] {

  def doRequest(simpleRequest: SimpleRequest): IO[SimpleResponse] =
    doRequestIO(simpleRequest)

  def doInteractionRequest(
                            url: String,
                            ir: InteractionRequest,
                          ): IO[InteractionResponse] =
    doInteractionRequestIO(url, ir)

  def doRequestSync(simpleRequest: SimpleRequest): Either[Throwable, SimpleResponse] =
    doRequestIO(simpleRequest).attempt.unsafeRunSync()

  def doInteractionRequestSync(
                                url: String,
                                ir: InteractionRequest,
                              ): Either[Throwable, InteractionResponse] =
    doInteractionRequestIO(url, ir).attempt.unsafeRunSync()

  def doRequestIO(simpleRequest: SimpleRequest): IO[SimpleResponse] = fetcher(simpleRequest, client)

  def doInteractionRequestIO(url: String, ir: InteractionRequest): IO[InteractionResponse] = {
    val request =
      SimpleRequest(
        url,
        ir.path.getOrElse("") + ir.query.map(q => s"?$q").getOrElse(""),
        HttpMethod.maybeMethodToMethod(ir.method),
        ir.headers.getOrElse(Map.empty[String, String]),
        ir.body,
        None,
      )
    fetcher(request, client).map { r =>
      InteractionResponse(
        status = Option(r.statusCode),
        headers =
          if (r.headers.isEmpty) None
          else Option(r.headers.map(p => p._1 -> p._2)),
        body = r.body,
        matchingRules = None
      )
    }
  }

}

object ScalaPactHttpClient {
  def apply(clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): IScalaPactHttpClient[IO] = {
    val client = Http4sClientHelper.buildPooledBlazeHttpClient(1, clientTimeout, sslContextName)
    new ScalaPactHttpClient(client)(Http4sClientHelper.doRequest)
  }
}
