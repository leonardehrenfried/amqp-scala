package io.relayr.amqp

import scala.concurrent.duration.FiniteDuration

sealed trait ReconnectionStrategy

object ReconnectionStrategy {
  val default = NoReconnect

  object NoReconnect extends ReconnectionStrategy

  case class JavaClientFixedReconnectDelay(networkRecoveryInterval: FiniteDuration) extends ReconnectionStrategy

  // trait DynamicReconnectionStrategy {
  //  def scheduleReconnection(f: ⇒ Unit): Unit
  //}
}

//case class ReconnectionStrategy(reconnectDelays: Stream[FiniteDuration], reconnectionExecutor: ExecutionContext)
