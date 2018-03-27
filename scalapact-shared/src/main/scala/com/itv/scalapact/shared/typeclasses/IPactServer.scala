package com.itv.scalapact.shared.typeclasses

trait IPactServer {

  def awaitShutdown(): Unit

  def shutdown(): Unit

}
