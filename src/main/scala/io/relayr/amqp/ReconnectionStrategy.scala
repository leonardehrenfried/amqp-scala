package io.relayr.amqp

object ReconnectionStrategy {
  val default: Option[ReconnectionStrategy] = None
}

trait ReconnectionStrategy {
  def scheduleReconnection(f: ⇒ Unit): Unit
}

//case class ReconnectionStrategy(reconnectDelays: Stream[FiniteDuration], reconnectionExecutor: ExecutionContext)
