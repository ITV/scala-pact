package com.itv.scalapact.shared

trait PactLogger {
  def message(s: Any): Unit
  def debug(s: Any): Unit
  def warn(s: Any): Unit
  def error(s: Any): Unit
}

object NullPactLogger extends PactLogger {
  def message(s: Any): Unit = ()
  def debug(s: Any): Unit   = ()
  def warn(s: Any): Unit    = ()
  def error(s: Any): Unit   = ()
}
object QuietPactLogger extends PactLogger {
  def message(s: Any): Unit = println(s)
  def debug(s: Any): Unit   = {}
  def warn(s: Any): Unit    = println(s)
  def error(s: Any): Unit   = println(s)
}

object NoisyPactLogger extends PactLogger {
  def message(s: Any): Unit = println(s)
  def debug(s: Any): Unit   = println(s)
  def warn(s: Any): Unit    = println(s)
  def error(s: Any): Unit   = println(s)
}

object PactLogger {
  private var logger: PactLogger = QuietPactLogger

  def nullLogger(): Unit  = logger = NullPactLogger
  def quietLogger(): Unit = logger = QuietPactLogger
  def noisyLogger(): Unit = logger = NoisyPactLogger

  def message(s: => String): Unit = logger.message(s)
  def debug(s: => String): Unit   = logger.debug(s)
  def warn(s: => String): Unit    = logger.warn(s)
  def error(s: => String): Unit   = logger.error(s)
}
