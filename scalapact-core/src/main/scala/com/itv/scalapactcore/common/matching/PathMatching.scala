package com.itv.scalapactcore.common.matching

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

import com.itv.scalapact.shared.Helpers

object PathMatching {

  case class PathAndQuery(path: Option[String], query: Option[String])

  def matchPaths(expected: PathAndQuery, received: PathAndQuery): MatchOutcome =
    matchPathsWithPredicate(expected, received) { (ex: PathStructure, re: PathStructure) =>
      {

        MatchOutcome.fromPredicate(
          ex.path == re.path,
          s"Paths '${ex.path}' and '${re.path}' did not match",
          25
        ) + equalListsOfParameters(ex.params, re.params)

      }
    }

  def matchPathsStrict(expected: PathAndQuery, received: PathAndQuery): MatchOutcome =
    matchPathsWithPredicate(expected, received) { (ex: PathStructure, re: PathStructure) =>
      {

        MatchOutcome.fromPredicate(
          ex.path == re.path,
          s"Paths '${ex.path}' and '${re.path}' did not match",
          20
        ) + MatchOutcome.fromPredicate(
          ex.params.length == re.params.length,
          s"Paths contained different numbers of parameters. Expected '${ex.params.length}' but got '${re.params.length}'",
          5
        ) + equalListsOfParameters(ex.params, re.params)

      }
    }

  private def matchPathsWithPredicate(expected: PathAndQuery, received: PathAndQuery)(
      predicate: (PathStructure, PathStructure) => MatchOutcome): MatchOutcome =
    GeneralMatcher.generalMatcher(
      (reconstructPath andThen convertToPathStructure)(expected),
      (reconstructPath andThen convertToPathStructure)(received),
      MatchOutcomeFailed("Paths do not match", 50),
      predicate
    )

  private def reconstructPath: PathAndQuery => String =
    pathAndQuery =>
      pathAndQuery.path.getOrElse("").split('?').toList ++ List(
        pathAndQuery.query.map(q => URLDecoder.decode(q, StandardCharsets.UTF_8.name())).getOrElse("")) match {
        case Nil =>
          "/"

        case x :: xs =>
          List(x, xs.filter(!_.isEmpty).mkString("&")).mkString("?")
    }

  private case class PathStructure(path: String, params: List[(String, String)])

  private object PathStructure {
    def empty: PathStructure = PathStructure("", Nil)
  }

  private def convertToPathStructure: String => Option[PathStructure] =
    fullPath =>
      if (fullPath.isEmpty) Option(PathStructure.empty)
      else {
        fullPath.split('?').toList match {
          case Nil =>
            Option(PathStructure.empty) // should never happen

          case x :: Nil =>
            Option(PathStructure(x, Nil))

          case x :: xs =>
            Option(
              PathStructure(
                path = x,
                params = Helpers.pairTuples(xs.mkString.split('&').toList.flatMap(p => p.split('=').toList))
              )
            )
        }
    }

  private def equalListsOfParameters(listA: List[(String, String)], listB: List[(String, String)]): MatchOutcome = {
    @annotation.tailrec
    def rec(remaining: List[((String, String), Int)],
            compare: List[((String, String), Int)],
            acc: List[MatchOutcome]): List[MatchOutcome] =
      remaining match {
        case Nil =>
          acc

        case x :: xs =>
          rec(
            xs,
            compare,
            MatchOutcome.fromPredicate(
              compare.exists(p => p._1._1 == x._1._1 && p._1._2 == x._1._2 && p._2 == x._2),
              s"No match for path param '${x._1._1}' with value '${x._1._2}' in position '${x._2}'",
              if (compare.nonEmpty) 25 / compare.length else 25
            ) :: acc
          )
      }

    listA
      .groupBy(_._1)
      .map { p =>
        rec(p._2.zipWithIndex, listB.groupBy(_._1).getOrElse(p._1, Nil).zipWithIndex, Nil)
      }
      .toList
      .flatten match {
      case Nil =>
        MatchOutcomeSuccess

      case x :: xs =>
        xs.foldLeft(x)(_ + _)
    }
  }

}
