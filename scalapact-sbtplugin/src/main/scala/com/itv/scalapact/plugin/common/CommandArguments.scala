package com.itv.scalapact.plugin.common

object CommandArguments {

  val parseArguments: Seq[String] => Arguments = args =>
    (Helpers.pair andThen convertToArguments)(args.toList)

  private lazy val convertToArguments: Map[String, String] => Arguments = argMap =>
    Arguments(
      host = argMap.get("--host"),
      port = argMap.get("--port").flatMap(Helpers.safeStringToInt),
      localPactPath = argMap.get("--source")
    )
}

object Helpers {

  val pair: List[String] => Map[String, String] = list => {
    @annotation.tailrec
    def rec[A](l: List[A], acc: List[Map[A, A]]): List[Map[A, A]] = {
      l match {
        case Nil => acc
        case x :: Nil => acc
        case x :: xs => rec(l.drop(2), Map(x -> xs.head) :: acc)
      }
    }

    rec(list, Nil).foldLeft(Map[String, String]())(_ ++ _)
  }

  val pairTuples: List[String] => List[(String, String)] = list => {
    @annotation.tailrec
    def rec[A](l: List[A], acc: List[(A, A)]): List[(A, A)] = {
      l match {
        case Nil => acc
        case x :: Nil => acc
        case x :: xs => rec(l.drop(2), (x, xs.head) :: acc)
      }
    }

    rec(list, Nil).foldLeft(List[(String, String)]())((a, b) => b :: a)
  }

  val safeStringToInt: String => Option[Int] = s =>
    try {
      Option(s.toInt)
    } catch {
      case e: Throwable => None
    }

}

case class Arguments(host: Option[String], port: Option[Int], localPactPath: Option[String]) {
  val giveHost = host.getOrElse("localhost")
  val givePort = port.getOrElse(1234)
}
