package io.relayr.amqp

import com.rabbitmq.client.ConnectionFactory
import io.relayr.amqp.connection.ConnectionHolderFactory

import scala.concurrent.ExecutionContext

/** Holds a connection, the underlying connection may be replaced if it fails */
trait ConnectionHolder {
  /** Create a new channel multiplexed over this connection */
  def newChannel(qos: Int): ChannelOwner

  /** Create a new channel multiplexed over this connection with a qos level set */
  def newChannel(): ChannelOwner

  def close()
}

object ConnectionHolder {
  case class Builder(
    connectionFactory: ConnectionFactory,
    executionContext: ExecutionContext,
    reconnectionStrategy: Option[ReconnectionStrategy] = ReconnectionStrategy.default,
    eventHooks: EventHooks = EventHooks(PartialFunction.empty)) extends ConnectionHolderFactory(connectionFactory, reconnectionStrategy, eventHooks, executionContext)
}