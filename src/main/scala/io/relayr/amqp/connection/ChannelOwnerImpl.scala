package io.relayr.amqp.connection

import com.rabbitmq.client._
import io.relayr.amqp._
import io.relayr.amqp.rpc.server.RPCServerImpl

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
   * @param listenQueue specifies the queue
   * @param handler function to call with RPC calls
   * @param ec executor for running the handler
   */
  override def rpcServer(listenQueue: Queue)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): RPCServer =
    new RPCServerImpl(this, listenQueue, ec, handler)

  override def addConsumer(queue: Queue, autoAck: Boolean, consumer: (Delivery) ⇒ Unit): Unit = withChannel { channel ⇒
    val queueName = ensureQueue(channel, queue)
    channel.basicConsume(queueName, autoAck, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        consumer(Delivery(properties, body))
      }
    })
  }

  override def createQueue(queue: QueueDeclare): QueueDeclared = withChannel { channel ⇒
    QueueDeclared(ensureQueue(channel, queue))
  }

  override def send(routingDescriptor: RoutingDescriptor, message: Message, basicProperties: BasicProperties): Unit = ???

  /**
   * Requests a queue declare on the channel, making sure we can use it and obtaining it's name (if we don't have that)
   */
  private def ensureQueue(channel: Channel, queue: Queue): String = queue match {
    case QueueDeclare(nameOpt, durable, exclusive, autoDelete, args) ⇒
      channel.queueDeclare(nameOpt.getOrElse(""), durable, exclusive, autoDelete, JavaConversions.mapAsJavaMap(args))
        .getQueue
    case QueuePassive(name) ⇒
      channel.queueDeclarePassive(name)
        .getQueue
  }
}

private[connection] object ChannelOwnerImpl extends ChannelFactory {
  def apply(cs: ChannelSessionProvider, executionContext: ExecutionContext) = new ChannelOwnerImpl(cs, executionContext)
}

