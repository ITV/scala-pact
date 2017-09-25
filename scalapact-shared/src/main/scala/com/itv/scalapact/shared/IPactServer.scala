package com.itv.scalapact.shared

trait IPactServer {

  def awaitShutdown(): Unit

  def shutdown(): Unit

}
