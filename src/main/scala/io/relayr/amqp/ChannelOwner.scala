package io.relayr.amqp

import io.relayr.amqp.rpc.server.ResponseParameters

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.higherKinds

/** Operation to perform on an amqp channel, the underlying connection may fail and be replaced by a new one with the same parameters */
trait ChannelOwner {
  def queueBind(queue: QueuePassive, exchange: Exchange, routingKey: String)

  /**
   * Sends a message over this channel
   * @param routingDescriptor describes how the Message will be routed, including, exchange, routing key and routing flags
   * @param message Message to send with properties and headers
   * @param onReturn callback in case message is returned undelivered
   * @param returnTimeout duration within which a return would be expected, messages returned after this time will not cause a callback
   */
  def send(routingDescriptor: RoutingDescriptor, message: Message, onReturn: () ⇒ Unit, returnTimeout: FiniteDuration): Unit

  /**
   * Sends a message over this channel
   * @param routingDescriptor describes how the Message will be routed, including, exchange, routing key and routing flags
   * @param message Message to send with properties and headers
   */
  def send(routingDescriptor: RoutingDescriptor, message: Message): Unit

  def sendPublish(publish: Publish): Unit =
    send(publish.routingDescriptor, publish.message)

  /**
   * Listen to a queue, the consumer is responsible for acking the messages
   */
  def addConsumerAckManual(queue: Queue, consumer: (Message, ManualAcker) ⇒ Unit): Closeable

  /**
   * Listens on the queue, automatically acknowledging messages and then passing them to the consumer
   */
  def addConsumer(queue: Queue, consumer: Message ⇒ Unit): Closeable

  def declareExchange(name: String, exchangeType: ExchangeType, durable: Boolean = false, autoDelete: Boolean = false, args: Map[String, AnyRef] = Map.empty): Exchange
  def declareExchangePassive(name: String): Exchange
  def declareQueue(queue: Queue): String

  /** Adds a handler to respond to RPCs on a particular binding */
  def rpcServer(listenQueue: Queue, ackMode: RpcServerAutoAckMode, responseParameters: ResponseParameters = ResponseParameters(mandatory = false, immediate = false, None))(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): Closeable
}
