package io.relayr.amqp

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.higherKinds

/** Operation to perform on an amqp channel, the underlying connection may fail and be replaced by a new one with the same parameters */
trait ChannelOwner {
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

  def declareQueue(queue: Queue): String

  /** Adds a handler to respond to RPCs on a particular binding */
  def rpcServer(listenQueue: Queue)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): Closeable
}
