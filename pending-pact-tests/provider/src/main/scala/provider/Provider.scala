package provider

import java.io.File

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.circe._
import org.http4s.util.CaseInsensitiveString
import io.circe.{Json, parser}

import scala.io.Source

object Provider {

  val service: HttpApp[IO] = HttpRoutes.of[IO] {
      case request @ GET -> Root / "results" =>
        makeResponse(request)
      case request @ GET -> Root / "results2" =>
        makeResponse(request)
  }.orNotFound

  def makeResponse(request: Request[IO]): IO[Response[IO]] = {
    val pactHeader = request.headers.get(CaseInsensitiveString("Pact")).map(_.value).getOrElse("")

    val body = {
      val source = Source.fromFile(new File("../consumer/master-expected.json"))
      val b = source.getLines().mkString
      source.close()
      parser.parse(b).getOrElse(Json.obj())
    }
    Ok(body).map(_.putHeaders(Header("Pact", pactHeader)))
  }

}
