package io.relayr.amqp

import com.rabbitmq.client.ConnectionFactory

import scala.concurrent.duration.FiniteDuration

class ConnectionHolderFactory(connectionFactory: ConnectionFactory, reconnectionStrategy: Stream[FiniteDuration]) {
  def newConnectionHolder(): ConnectionHolder = ???
}
