package io.relayr.amqp

import com.rabbitmq.client.AMQP

import scala.concurrent.{ Future, ExecutionContext }

/** Operation to perform on an amqp channel, the underlying connection may fail and be replaced by a new one with the same parameters */
trait ChannelOwner {
  def send(routingDescriptor: RoutingDescriptor, message: Message, basicProperties: AMQP.BasicProperties = new AMQP.BasicProperties()): Unit

  def sendPublish(publish: Publish): Unit =
    send(publish.routingDescriptor, publish.message, publish.properties)

  def addConsumer(queue: Queue, autoAck: Boolean, consumer: (Delivery) ⇒ Unit): Closeable

  def declareQueue(queue: Queue): String

  /** Adds a handler to respond to RPCs on a particular binding */
  def rpcServer(listenQueue: Queue)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): Closeable
}
