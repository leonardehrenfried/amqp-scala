package io.relayr.amqp

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{ AMQP, ConnectionFactory }
import io.relayr.amqp.connection.ConnectionHolderFactory

import scala.concurrent.{ ExecutionContext, Future }

/** Defines a queue to connect to or create */
sealed trait Queue
/** Describes an exchange which should already exist, an error is thrown if it does not */
case class QueuePassive(name: String) extends Queue
/** Parameters to create a new queue */
case class QueueDeclare(name: Option[String], durable: Boolean = false, exclusive: Boolean = false, autoDelete: Boolean = true, args: Map[String, AnyRef] = Map.empty) extends Queue

/** All parameters to set up a queue to listen for messages on */
case class Binding(exchangeParameters: Exchange, queueParameters: Queue, routingKey: String)

/** The queue server found nowhere to route the message */
case class UndeliveredException() extends Exception

/** Message blob with content headers */
case class Message(contentType: String, contentEncoding: String, body: ByteArray)

/** Operation to perform on an amqp channel, the underlying connection may fail and be replaced by a new one with the same parameters */
trait ChannelOwner {
  def send(routingDescriptor: RoutingDescriptor, message: Message, basicProperties: AMQP.BasicProperties = new AMQP.BasicProperties()): Unit

  def sendPublish(publish: Publish): Unit =
    send(publish.routingDescriptor, publish.message, publish.properties)

  def addConsumer(queue: Queue, autoAck: Boolean, consumer: (Delivery) ⇒ Unit): Closeable

  def declareQueue(queue: Queue): QueueDeclared

  /** Adds a handler to respond to RPCs on a particular binding */
  def rpcServer(listenQueue: Queue)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): RPCServer
}

/** Closeable for a handler of RPCs, close to stop the handler from being called */
trait RPCServer {
  def close(): Unit
}

case class RoutingDescriptor(exchange: ExchangePassive, routingKey: String, deliveryMode: DeliveryMode)

sealed abstract class DeliveryMode(val value: Int)

object DeliveryMode {
  case object NotPersistent extends DeliveryMode(1)
  case object Persistent extends DeliveryMode(2)
}

/** Holds a connection, the underlying connection may be replaced if it fails */
trait ConnectionHolder {
  /** Create a new channel multiplexed over this connection */
  def newChannel(qos: Int): ChannelOwner

  /** Create a new channel multiplexed over this connection with a qos level set */
  def newChannel(): ChannelOwner

  def close()
}

case class Publish(routingDescriptor: RoutingDescriptor, message: Message, properties: AMQP.BasicProperties = new BasicProperties())

case class ConnectionHolderBuilder(
  connectionFactory: ConnectionFactory,
  executionContext: ExecutionContext,
  reconnectionStrategy: Option[ReconnectionStrategy] = ReconnectionStrategy.default,
  eventHooks: EventHooks = EventHooks()) extends ConnectionHolderFactory(connectionFactory, reconnectionStrategy, eventHooks, executionContext)

case class QueueDeclared(name: String)

trait Closeable {
  def close(): Unit
}
