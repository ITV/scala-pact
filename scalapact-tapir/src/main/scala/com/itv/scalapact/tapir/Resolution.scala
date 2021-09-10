package com.itv.scalapact.tapir

import cats._

import scala.collection.immutable.ListMap
import cats.implicits._
import sttp.tapir.apispec.SchemaType.SchemaType
import sttp.tapir.apispec.{ReferenceOr, Schema}
import sttp.tapir.openapi.{Components, MediaType, OpenAPI, Operation, PathItem, Response, ResponsesKey}

object Resolution {

  implicit def listMapTraverse[K]: Traverse[({ type L[X] = ListMap[K, X] })#L] =
    new Traverse[({ type L[X] = ListMap[K, X] })#L] {
      override def traverse[G[_], A, B](
          fa: ListMap[K, A]
      )(f: A => G[B])(implicit evidence$1: Applicative[G]): G[ListMap[K, B]] = fa.toList
        .traverse { case (k, v) =>
          f(v).map(k -> _)
        }
        .map(l => ListMap(l: _*))

      override def foldLeft[A, B](fa: ListMap[K, A], b: B)(f: (B, A) => B): B =
        Foldable[List].foldLeft(fa.values.toList, b)(f)

      override def foldRight[A, B](fa: ListMap[K, A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
        Foldable[List].foldRight(fa.values.toList, lb)(f)
    }

  case class ResolvedOpenApi(paths: ListMap[String, ResolvedPathItem])

  case class ResolvedPathItem(
      get: Option[ResolvedOperation],
      put: Option[ResolvedOperation],
      post: Option[ResolvedOperation],
      patch: Option[ResolvedOperation],
      delete: Option[ResolvedOperation]
  )
  case class ResolvedOperation(responses: ListMap[ResponsesKey, ResolvedResponse])

  case class ResolvedResponse(content: ListMap[String, ResolvedMediaType])
  case class ResolvedMediaType(schema: ResolvedSchema)
  case class ResolvedSchema(
      title: Option[String],
      `type`: SchemaType,
      properties: ListMap[String, ResolvedSchema],
      items: Option[ResolvedSchema]
  )

  def resolvedOpenApi(openAPI: OpenAPI): Either[Throwable, ResolvedOpenApi] = openAPI.paths
    .traverse {
      resolvePathItem(_, openAPI.components)
    }
    .map(ResolvedOpenApi)

  def resolvePathItem(item: PathItem, maybeComponents: Option[Components]): Either[Throwable, ResolvedPathItem] = (
    item.get.traverse(resolveOperation(_, maybeComponents)),
    item.put.traverse(resolveOperation(_, maybeComponents)),
    item.post.traverse(resolveOperation(_, maybeComponents)),
    item.patch.traverse(resolveOperation(_, maybeComponents)),
    item.delete.traverse(resolveOperation(_, maybeComponents))
  ).mapN(ResolvedPathItem)

  def resolveOperation(
      operation: Operation,
      maybeComponents: Option[Components]
  ): Either[Throwable, ResolvedOperation] = operation.responses
    .traverse(
      _.leftMap(_ => new Exception("doesn't support referenced responses")).flatMap(resolveResponse(_, maybeComponents))
    )
    .map(ResolvedOperation)

  def resolveMediaType(
      mediaType: MediaType,
      maybeComponents: Option[Components]
  ): Either[Throwable, ResolvedMediaType] = mediaType.schema
    .toRight(new Exception("no schema"))
    .flatMap(resolveSchema(_, maybeComponents))
    .map(ResolvedMediaType)

  def resolveResponse(response: Response, maybeComponents: Option[Components]): Either[Throwable, ResolvedResponse] =
    response.content.traverse(resolveMediaType(_, maybeComponents)).map(ResolvedResponse)

  def resolveSchema(
      schema: ReferenceOr[Schema],
      maybeComponents: Option[Components]
  ): Either[Throwable, ResolvedSchema] = schema match {
    case Left(reference) =>
      maybeComponents
        .flatMap(_.schemas.get(reference.$ref.split('/').last))
        .toRight(new Exception(s"no schema $reference"))
        .flatMap(resolveSchema(_, maybeComponents))
    case Right(schema) =>
      (
        schema.`type`.toRight(new Exception(s"schema $schema had no type")),
        schema.properties.traverse(resolveSchema(_, maybeComponents)),
        schema.items.traverse(resolveSchema(_, maybeComponents))
      ).mapN(ResolvedSchema(schema.title, _, _, _))
  }

}
