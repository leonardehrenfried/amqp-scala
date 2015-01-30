package io.relayr.amqp.connection

import java.util.Date

import com.rabbitmq.client.{ AMQP, Channel, DefaultConsumer, Envelope }
import io.relayr.amqp._
import io.relayr.amqp.properties.Key
import io.relayr.amqp.rpc.server.RPCServerImpl

import scala.collection.JavaConversions
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.higherKinds

/**
 * Provides different use cases for a channel
 * @param cs provides the channel to be used for these strategies, after a reconnection of the underlying connection this channel would change
 */
private[connection] class ChannelOwnerImpl(cs: ChannelSessionProvider) extends ChannelOwner {
  def withChannel[T]: ((Channel) ⇒ T) ⇒ T = cs.withChannel

  /**
   * Adds a handler to respond to RPCs on a particular binding
   * @param listenQueue specifies the queue
   * @param handler function to call with RPC calls
   * @param ec executor for running the handler
   */
  override def rpcServer(listenQueue: Queue)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): Closeable =
    new RPCServerImpl(this, listenQueue, ec, handler)

  override def addConsumerAckManual(queue: Queue, consumer: (Message, ManualAcker) ⇒ Unit): Closeable = withChannel { channel ⇒
    val queueName = ensureQueue(channel, queue)
    val consumerTag = channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        consumer(Message.Raw(body, properties), new ManualAcker {
          override def reject(requeue: Boolean): Unit = channel.basicReject(envelope.getDeliveryTag, requeue)

          override def ack(): Unit = channel.basicAck(envelope.getDeliveryTag, false)
        })
      }
    })
    new ConsumerCloser(consumerTag)
  }

  override def addConsumer(queue: Queue, consumer: Message ⇒ Unit): Closeable = withChannel { channel ⇒
    val queueName = ensureQueue(channel, queue)
    val consumerTag = channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        consumer(Message.Raw(body, properties))
      }
    })
    new ConsumerCloser(consumerTag)
  }

  override def declareQueue(queue: Queue): String = withChannel { channel ⇒
    ensureQueue(channel, queue)
  }

  override def send(routingDescriptor: RoutingDescriptor, message: Message): Unit = withChannel { channel ⇒
    val Message.Raw(array, basicProperties) = message.withProperties(
      Key.Timestamp → new Date(),
      Key.DeliveryMode → routingDescriptor.deliveryMode.value)
    channel.basicPublish(routingDescriptor.exchange.name, routingDescriptor.routingKey, false, false, basicProperties, array)
  }

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

  class ConsumerCloser(consumerTag: String) extends Closeable {
    def close(): Unit = withChannel { channel ⇒
      channel.basicCancel(consumerTag)
    }
  }
}

private[amqp] object ChannelOwnerImpl extends ChannelFactory {
  def apply(cs: ChannelSessionProvider) = new ChannelOwnerImpl(cs)
}

