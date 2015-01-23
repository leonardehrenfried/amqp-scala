package io.relayr.amqp

import com.rabbitmq.client.ConnectionFactory
import io.relayr.amqp.connection.ConnectionHolderFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }

/** Defines an exchange to connect to or create */
sealed trait Exchange
/** Describes an exchange which should already exist, an error is thrown if it does not */
case class ExchangePassive(name: String) extends Exchange
/** Parameters to create a new exchange */
case class ExchangeDeclare(name: String, exchangeType: String, durable: Boolean = false, autodelete: Boolean = false, args: Map[String, AnyRef] = Map.empty) extends Exchange

/** Defines a queue to connect to or create */
sealed trait Queue
/** Describes an exchange which should already exist, an error is thrown if it does not */
case class QueuePassive(name: String) extends Queue
/** Parameters to create a new queue */
case class QueueDeclare(name: String, durable: Boolean = false, exclusive: Boolean = false, autodelete: Boolean = true, args: Map[String, AnyRef] = Map.empty) extends Queue

/** All parameters to set up a queue to listen for messages on */
case class Binding(exchangeParameters: Exchange, queueParameters: Queue, routingKey: String)

/** The queue server found nowhere to route the message */
case class UndeliveredException() extends Exception

/** Message blob with content headers */
case class Message(contentType: String, contentEncoding: String, body: ByteArray)

/** Operation to perform on an amqp channel, the underlying connection may fail and be replaced by a new one with the same parameters */
trait ChannelOwner {
  /** Adds a handler to respond to RPCs on a particular binding */
  def rpcServer(binding: Binding)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): RPCServer

  /** Creates a client for making RPCs via amqp */
  def rpcClient(exchange: String, routingKey: String)(deliveryMode: DeliveryMode): RPCClient
}

/** Makes RPCs to a particular exchange + routing key combo with set DeliveryMode */
trait RPCClient {
  /** Possible exceptions are TimeoutException and UndeliveredException */
  def apply(message: Message)(implicit timeout: FiniteDuration): Future[Message]
}

/** Closeable for a handler of RPCs, close to stop the handler from being called */
trait RPCServer {
  def close(): Unit
}

sealed abstract class DeliveryMode(val value: Int)

object DeliveryMode {
  case object NotPersistent extends DeliveryMode(1)
  case object Persistent extends DeliveryMode(2)
}

/** Holds a connection, the underlying connection may be replaced if it fails */
trait ConnectionHolder {
  /** Create a new channel multiplexed over this connection */
  def newChannel(qos: Int): ChannelOwner

  def close()
}

case class ConnectionHolderBuilder(
  connectionFactory: ConnectionFactory,
  executionContext: ExecutionContext,
  reconnectionStrategy: Option[ReconnectionStrategy] = ReconnectionStrategy.default,
  eventHooks: EventHooks = EventHooks()) extends ConnectionHolderFactory(connectionFactory, reconnectionStrategy, eventHooks, executionContext)
