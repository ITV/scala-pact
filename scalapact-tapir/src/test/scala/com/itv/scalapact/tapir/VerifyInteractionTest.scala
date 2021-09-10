package com.itv.scalapact.tapir

import cats.implicits.catsSyntaxOptionId
import com.itv.scalapact.shared.{Interaction, InteractionRequest, InteractionResponse}
import io.circe
import io.circe.generic.semiauto.deriveCodec
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import sttp.model.StatusCode
import sttp.tapir.endpoint
import sttp.tapir._
import sttp.tapir
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.openapi.OpenAPI

class VerifyInteractionTest extends AnyFreeSpec with Matchers with ScalaCheckDrivenPropertyChecks {
  val body = """{ "paws": 4, "name": "Oscar"}"""
  val interaction =
    Interaction(
      None,
      "My Interaction",
      InteractionRequest("GET".some, "/path/to/cat/Oscar".some, None, None, None, None),
      InteractionResponse(Some(StatusCode.Created.code), None, body.some, None)
    )

  "a" in { final case class Cat(paws: Int, name: String)
    sealed trait Error
    final case class NewCat(cat: Cat, dateOfBirth: String)
    case object NoCat                        extends Error
    final case class BadReq(message: String) extends Error
    object BadReq {
      implicit val code: circe.Codec[BadReq] = deriveCodec
    }
    object Cat {
      implicit val jsonCodec: circe.Codec[Cat] = deriveCodec
    }

    val endp: Endpoint[String, Error, Cat, Any] = endpoint.get
      .in("path" / "to" / "cat" / path[String])
      .out(jsonBody[Cat])
      .out(statusCode(StatusCode.Created))
      .errorOut(
        tapir.oneOf[Error](
          statusMapping(StatusCode.NotFound, emptyOutput.map(_ => NoCat)(_ => ())),
          statusMapping(StatusCode.BadRequest, jsonBody[BadReq])
        )
      )

    val openApi: OpenAPI = OpenAPIDocsInterpreter.toOpenAPI(List(endp), "title", "1.0")

    Tapir.verifyPact(interaction, Resolution.resolvedOpenApi(openApi).right.get) shouldBe a[Right[_,_]]
  }

}
