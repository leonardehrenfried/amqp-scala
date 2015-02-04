package io.relayr.amqp

import scala.concurrent.duration.FiniteDuration

sealed trait ReconnectionStrategy

/**
 * Strategies for reconnection, currently only fixed delay or no reconnection are available
 */
object ReconnectionStrategy {
  val default = NoReconnect

  object NoReconnect extends ReconnectionStrategy

  /**
   * Uses the underlying java client to attempt network recovery at a fixed interval
   * @param networkRecoveryInterval duration between reconnection attempts, minimum resolution in millis
   */
  case class JavaClientFixedReconnectDelay(networkRecoveryInterval: FiniteDuration) extends ReconnectionStrategy

  // trait DynamicReconnectionStrategy {
  //  def scheduleReconnection(f: ⇒ Unit): Unit
  //}
}

//case class ReconnectionStrategy(reconnectDelays: Stream[FiniteDuration], reconnectionExecutor: ExecutionContext)
