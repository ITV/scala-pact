import cats.implicits.catsSyntaxOptionId
import io.circe.parser
import com.itv.scalapact.shared.{Interaction, InteractionRequest, InteractionResponse}
import sttp.tapir.openapi.OpenAPI

import scala.util.matching.Regex

//def pathPatternToRegex(pathPattern: String): Regex =
//  pathPattern
//    .split('/')
//    .map(segment => if (segment.startsWith("{") && segment.endsWith("}")) "[^\\/]*" else segment)
//    .mkString("\\/")
//    .r
//
//val regex = pathPatternToRegex("/path/to/cats/{p1}/here")
//
//"/path/to/cats/oscar/here" match {
//  case regex() => print("Yup")
//  case _       => println("Nope")
//}

val body = """{ "paws": 4, "name": "Oscar"}"""
val interaction =
  Interaction(
    None,
    "My Interaction",
    InteractionRequest("GET".some, "/path/to/cat/Oscar".some, None, None, None, None),
    InteractionResponse(None, None, body.some, None)
  )
