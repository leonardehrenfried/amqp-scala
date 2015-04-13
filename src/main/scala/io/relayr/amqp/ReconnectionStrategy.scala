package io.relayr.amqp

import net.jodah.lyra

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
   *
   * NOTE: The java client only supports recovery of an entire connection when it is lost due to a network failure,
   * channels and consumers can also fail (due to things like using an exchange that doesn't exist on the broker) -
   * these will not be recovered.
   *
   * @param networkRecoveryInterval duration between reconnection attempts, minimum resolution in millis
   */
  case class JavaClientFixedReconnectDelay(networkRecoveryInterval: FiniteDuration) extends ReconnectionStrategy

  /**
   * Uses Lyra to build a topology recovery https://github.com/jhalterman/lyra . Lyra provides many options for recovery strategies and can recover closed connections, channels or consumers.
   * @param config a lyra Config
   */
  case class LyraRecoveryStrategy(config: lyra.config.Config) extends ReconnectionStrategy
}

//case class ReconnectionStrategy(reconnectDelays: Stream[FiniteDuration], reconnectionExecutor: ExecutionContext)
