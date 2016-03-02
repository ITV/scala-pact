package com.itv.scalapact.plugin.stubber

object StubArguments {
  lazy val parseArguments: Seq[String] => Arguments = args =>
    (Convertors.pair andThen buildConfigMap)(args.toList)

  private lazy val buildConfigMap: Map[String, String] => Arguments = argMap =>
    Arguments(
      host = argMap.getOrElse("--host", "localhost"),
      port = argMap.get("--port").flatMap(safeStringToInt).getOrElse(1234),
      localPactPath = argMap.get("--source")
    )

  private lazy val safeStringToInt: String => Option[Int] = s =>
    try {
      Option(s.toInt)
    } catch {
      case e: Throwable => None
    }
}

object Convertors {

  lazy val pair: List[String] => Map[String, String] = list => {
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

}

case class Arguments(host: String, port: Int, localPactPath: Option[String])