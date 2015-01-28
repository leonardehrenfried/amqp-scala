package io.relayr.amqp.connection

import com.rabbitmq.client.ConnectionFactory
import io.relayr.amqp.{ ConnectionHolder, EventHooks, ReconnectionStrategy }

import scala.concurrent.ExecutionContext

private[amqp] class ConnectionHolderFactory(connectionFactory: ConnectionFactory, reconnectionStrategy: Option[ReconnectionStrategy], eventHooks: EventHooks, executionContext: ExecutionContext) {
  def newConnectionHolder(): ConnectionHolder = new ReconnectingConnectionHolder(connectionFactory, reconnectionStrategy, eventHooks.event, executionContext, ChannelOwnerImpl)
}
