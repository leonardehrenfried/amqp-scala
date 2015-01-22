package io.relayr.amqp.connection

import com.rabbitmq.client.ConnectionFactory
import io.relayr.amqp.ConnectionHolder

import scala.concurrent.duration.FiniteDuration

class ConnectionHolderFactory(connectionFactory: ConnectionFactory, reconnectionStrategy: Stream[FiniteDuration]) {
  def newConnectionHolder(): ConnectionHolder = ???
}
