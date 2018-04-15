package com.itv.scalapact.http4s17.impl

import com.itv.scalapact.shared.typeclasses.IScalaPactHttpClient
import com.itv.scalapact.shared.{SimpleRequest, _}
import fs2.{Strategy, Task}
import javax.net.ssl.SSLContext
import org.http4s.client.Client

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.{implicitConversions, postfixOps}

object ScalaPactHttpClient extends ScalaPactHttpClient[Client] {
  override def buildClient = new BuildClient[Client] {
    override def apply(maxTotalConnections: Int, clientTimeout: Duration, sslContext: Option[SSLContext]) =
      Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, clientTimeout, sslContext)
  }

  override def doRequest = Http4sClientHelper.doRequest
}

trait BuildClient[Client] {
  def apply(maxTotalConntections: Int, clientTimeout: Duration, sslContext: Option[SSLContext]): Client

}

trait ScalaPactHttpClient[Client] extends IScalaPactHttpClient {

  def buildClient: BuildClient[Client]

  def doRequest: (SimpleRequest, Client) => Task[SimpleResponse]

  implicit def taskToFuture[A](x: => Task[A]): Future[A] = {

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

  private val maxTotalConnections: Int = 1

  private implicit val strategy: Strategy = Http4sClientHelper.strategy

  def doRequest(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Future[SimpleResponse] =
    doRequestTask(doRequest, simpleRequest)

  def doInteractionRequest(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Future[InteractionResponse] =
    doInteractionRequestTask(doRequest, url, ir, clientTimeout, sslContextName: Option[String])

  def doRequestSync(simpleRequest: SimpleRequest)(
      implicit sslContextMap: SslContextMap): Either[Throwable, SimpleResponse] =
    doRequestTask(doRequest, simpleRequest).unsafeAttemptRun()

  def doInteractionRequestSync(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse] =
    doInteractionRequestTask(doRequest, url, ir, clientTimeout, sslContextName).unsafeAttemptRun()

  def doRequestTask(performRequest: (SimpleRequest, Client) => Task[SimpleResponse], simpleRequest: SimpleRequest)(
      implicit sslContextMap: SslContextMap): Task[SimpleResponse] =
    SslContextMap(simpleRequest)(sslContext =>
      simpleRequestWithoutFakeheader =>
        performRequest(simpleRequestWithoutFakeheader, buildClient(maxTotalConnections, 2.seconds, sslContext)))

  def doInteractionRequestTask(
      performRequest: (SimpleRequest, Client) => Task[SimpleResponse],
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Task[InteractionResponse] =
    SslContextMap(
      SimpleRequest(
        url,
        ir.path.getOrElse("") + ir.query.map(q => s"?$q").getOrElse(""),
        HttpMethod.maybeMethodToMethod(ir.method),
        ir.headers.getOrElse(Map.empty[String, String]),
        ir.body,
        sslContextName
      )) { sslContext => simpleRequestWithoutFakeheader =>
      performRequest(
        simpleRequestWithoutFakeheader,
        buildClient(maxTotalConnections, clientTimeout, sslContext)
      ).map { r =>
        InteractionResponse(
          status = Option(r.statusCode),
          headers = if (r.headers.isEmpty) None else Option(r.headers.map(p => p._1 -> p._2.mkString)),
          body = r.body,
          matchingRules = None
        )
      }
    }

}
