package io.relayr.amqp.connection

import com.rabbitmq.client.ConnectionFactory
import io.relayr.amqp.{ ReconnectionStrategy, EventHooks, ConnectionHolder }

import scala.concurrent.duration.FiniteDuration

private[amqp] class ConnectionHolderFactory(connectionFactory: ConnectionFactory, reconnectionStrategy: Option[ReconnectionStrategy], eventHooks: EventHooks) {
  def newConnectionHolder(): ConnectionHolder = new ReconnectingConnectionHolder(connectionFactory, reconnectionStrategy, eventHooks)
}
