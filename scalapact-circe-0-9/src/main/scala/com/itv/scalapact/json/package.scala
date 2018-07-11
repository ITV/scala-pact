package com.itv.scalapact

import com.itv.scalapact.circe09.{PactReader, PactWriter}
import com.itv.scalapact.shared.MessageContentType
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared.typeclasses._
import io.circe.{Json, JsonNumber, JsonObject}
import io.circe.parser.parse

package object json {
  implicit val pactReaderInstance: IPactReader =
    new PactReader

  implicit val pactWriterInstance: IPactWriter =
    new PactWriter

  implicit val jsonMessageFormatInstance: IMessageFormat[Json] = new IMessageFormat[Json] {
    override def contentType: MessageContentType = ApplicationJson

    override def encode(t: Json): String = t.noSpaces

    override def decode(s: String): Either[MessageFormatError, Json] =
      parse(s).fold(m => Left(MessageFormatError(m.message)), Right(_))
  }

  implicit val inferTypeInstance: IInferTypes[Json] = new IInferTypes[Json] {
    override protected def inferFrom(t: Json): Map[String, String] = typesFrom(".", t, Set.empty).toMap

    private def typesFrom(path: String, json: Json, acc: Set[(String, String)]): Set[(String, String)] = {

      def typesFromJsonObject(path: String, acc: Set[(String, String)])(json: JsonObject): Set[(String, String)] = {
        def typeNumber(jsonNumber: JsonNumber): String =
          jsonNumber.toLong.map(_ => "integer").getOrElse("decimal")

        json.keys
          .flatMap(
            key =>
              json(key).fold(acc) { jsonValue =>
                val currentPath                           = s"$path$key"
                def pair(value: String): (String, String) = currentPath -> value

                jsonValue.fold(
                  Set.empty,
                  _ => Set.empty,
                  value => Set(pair(typeNumber(value))),
                  _ => Set.empty,
                  _ => Set.empty,
                  x => typesFromJsonObject(s"$currentPath.", Set(pair("object")) ++ acc)(x)
                )
            }
          )
          .toSet
      }
      json.asObject.fold(acc)(typesFromJsonObject(path, acc))
    }
  }
}
