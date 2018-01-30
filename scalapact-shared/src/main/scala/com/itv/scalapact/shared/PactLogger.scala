package com.itv.scalapact.shared

trait PactLogger {
  def message(s: Any)
  def debug(s: Any)
  def warn(s: Any)
  def error(s: Any)
}

object NullPactLogger extends PactLogger {
  def message(s: Any) = {}
  def debug(s: Any) = {}
  def warn(s: Any) = {}
  def error(s: Any) = {}
}
object QuietPactLogger extends PactLogger {
  def message(s: Any) = println(s)
  def debug(s: Any) = {}
  def warn(s: Any) =println(s)
  def error(s: Any) =println(s)
}

object NoisyPactLogger extends PactLogger {
  def message(s: Any) = println(s)
  def debug(s: Any) = println(s)
  def warn(s: Any) = println(s)
  def error(s: Any) =println(s)
}

object PactLogger {
  private var logger: PactLogger = QuietPactLogger

  def nullLOgger = logger = NullPactLogger
  def quietLOgger = logger = QuietPactLogger
  def noisyLOgger = logger = NoisyPactLogger

  def message(s: => String) =logger.message(s)
  def debug(s: => String) = logger.debug(s)
  def warn(s: => String) = logger.warn(s)
  def error(s: => String) = logger.error(s)
}