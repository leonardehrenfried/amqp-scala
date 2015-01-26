package io.relayr.amqp.connection

import com.rabbitmq.client._
import io.relayr.amqp._
import io.relayr.amqp.rpc.client.Delivery

import scala.collection.JavaConversions
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Provides different use cases for a channel
 * @param cs provides the channel to be used for these strategies, after a reconnection of the underlying connection this channel would change
 * @param executionContext for any blocking channel management calls to the underlying java client
 */
private[connection] class ChannelOwnerImpl(cs: ChannelSessionProvider, executionContext: ExecutionContext) extends ChannelOwner {
  def withChannel[T]: ((Channel) ⇒ T) ⇒ T = cs.withChannel

  /**
   * Adds a handler to respond to RPCs on a particular binding
   * @param binding specifies the exxchange, queue and routing key to bind the listener of the rpcServer to
   * @param handler function to call with RPC calls
   * @param ec executor for running the handler
   */
  override def rpcServer(binding: Binding)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): RPCServer = ???

  override def addConsumer(queueName: String, autoAck: Boolean, consumer: (Delivery) ⇒ Unit): Unit = withChannel { channel ⇒
    def buildDelivery(properties: AMQP.BasicProperties, body: Array[Byte]): DeliveryImpl =
      DeliveryImpl(Message(properties.getContentType, properties.getContentEncoding, ByteArray(body)), properties.getCorrelationId)
    channel.basicConsume(queueName, autoAck, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        consumer(buildDelivery(properties, body))
      }
    })
  }

  override def createQueue(queue: QueueDeclare): QueueDeclared = withChannel { channel ⇒
    val declareOk = channel.queueDeclare(queue.name.getOrElse(""), queue.durable, queue.exclusive, queue.autodelete, JavaConversions.mapAsJavaMap(queue.args))
    QueueDeclared(declareOk.getQueue)
  }
}

private[connection] object ChannelOwnerImpl extends ChannelFactory {
  def apply(cs: ChannelSessionProvider, executionContext: ExecutionContext) = new ChannelOwnerImpl(cs, executionContext)
}

case class DeliveryImpl(message: Message, correlationId: String) extends Delivery
