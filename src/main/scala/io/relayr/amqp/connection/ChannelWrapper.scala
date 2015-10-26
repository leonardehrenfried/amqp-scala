package io.relayr.amqp.connection

import java.util.Date

import com.rabbitmq.client._
import com.rabbitmq.client.{ Envelope ⇒ JavaEnvelope }
import io.relayr.amqp._
import io.relayr.amqp.concurrent.ScheduledExecutor
import io.relayr.amqp.properties.Key
import io.relayr.amqp.rpc.server.{ RPCServerImpl, ResponseParameters }
import io.relayr.amqp.{ Envelope ⇒ OurEnvelope }

import scala.collection.JavaConversions
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.higherKinds

/**
 * Provides different use cases for a channel
 */
private[connection] class ChannelWrapper(channel: Channel, eventConsumer: Event ⇒ Unit, scheduledExecutor: ScheduledExecutor) extends ChannelOwner {
  val returnHandler = new ReturnedMessageHandler(scheduledExecutor)
  channel.addReturnListener(returnHandler.newReturnListener())

  /**
   * Adds a handler to respond to RPCs on a particular binding
   * @param listenQueue specifies the queue
   * @param handler function to call with RPC calls
   * @param ec executor for running the handler
   */
  override def rpcServer(listenQueue: Queue, ackMode: RpcServerAutoAckMode, responseParameters: ResponseParameters)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): Closeable =
    new RPCServerImpl(this, listenQueue, ackMode, eventConsumer, ec, handler, responseParameters)

  override def addConsumerAckManual(queue: Queue, consumer: (Message, ManualAcker) ⇒ Unit): Closeable = {
    val queueName = ensureQueue(channel, queue)
    val consumerTag = channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: JavaEnvelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        consumer(Message.Raw(body, properties), new ManualAcker {
          override def reject(requeue: Boolean): Unit = channel.basicReject(envelope.getDeliveryTag, requeue)

          override def ack(): Unit = channel.basicAck(envelope.getDeliveryTag, false)
        })
      }
    })
    new ConsumerCloser(consumerTag)
  }

  override def addEnvelopeConsumerAckManual(queue: Queue, consumer: (OurEnvelope, ManualAcker) ⇒ Unit): Closeable = {
    val queueName = ensureQueue(channel, queue)
    val consumerTag = channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: JavaEnvelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        consumer(OurEnvelope(envelope.getExchange, envelope.getRoutingKey, Message.Raw(body, properties)), new ManualAcker {
          override def reject(requeue: Boolean): Unit = channel.basicReject(envelope.getDeliveryTag, requeue)

          override def ack(): Unit = channel.basicAck(envelope.getDeliveryTag, false)
        })
      }
    })
    new ConsumerCloser(consumerTag)
  }

  override def addConsumer(queue: Queue, consumer: Message ⇒ Unit): Closeable = {
    val queueName = ensureQueue(channel, queue)
    val consumerTag = channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: JavaEnvelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        consumer(Message.Raw(body, properties))
      }
    })
    new ConsumerCloser(consumerTag)
  }

  override def addEnvelopeConsumer(queue: Queue, consumer: OurEnvelope ⇒ Unit): Closeable = {
    val queueName = ensureQueue(channel, queue)
    val consumerTag = channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: JavaEnvelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        consumer(OurEnvelope(envelope.getExchange, envelope.getRoutingKey, Message.Raw(body, properties)))
      }
    })
    new ConsumerCloser(consumerTag)
  }

  override def declareQueue(queue: Queue): String = {
    ensureQueue(channel, queue)
  }

  override def send(route: RoutingDescriptor, message: Message, onReturn: () ⇒ Unit, returnTimeout: FiniteDuration): Unit = {
    val messageIdProperty = Key.MessageId → returnHandler.setupReturnCallback(onReturn, returnTimeout)
    def timestampProperty = Key.Timestamp → new Date()
    val deliveryModeProperty = route.deliveryMode.map(Key.DeliveryMode → _)

    val Message.Raw(array, basicProperties) = message.withProperties(
      messageIdProperty +: timestampProperty +: deliveryModeProperty.toSeq: _*)
    channel.basicPublish(route.exchange.name, route.routingKey, route.mandatory, route.immediate, basicProperties, array)
  }

  override def send(route: RoutingDescriptor, message: Message): Unit = {
    def timestampProperty = Key.Timestamp → new Date()
    val deliveryModeProperty = route.deliveryMode.map(Key.DeliveryMode → _)

    val Message.Raw(array, basicProperties) = message.withProperties(
      timestampProperty +: deliveryModeProperty.toSeq: _*)
    channel.basicPublish(route.exchange.name, route.routingKey, route.mandatory, route.immediate, basicProperties, array)
  }

  /**
   * Requests an exchange declare on the channel, making sure we can use it
   */
  override def declareExchange(name: String, exchangeType: ExchangeType, durable: Boolean = false, autoDelete: Boolean = false, args: Map[String, AnyRef] = Map.empty): Exchange = {
    channel.exchangeDeclare(name, exchangeType.name, durable, autoDelete, JavaConversions.mapAsJavaMap(args))
    Exchange(name)
  }

  override def declareExchangePassive(name: String) = {
    channel.exchangeDeclarePassive(name)
    Exchange(name)
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
    def close(): Unit = {
      channel.basicCancel(consumerTag)
    }
  }

  def queueBind(queue: QueuePassive, exchange: Exchange, routingKey: String): Unit = {
    channel.queueBind(queue.name, exchange.name, routingKey)
  }

  override def close(): Unit = {
    channel.close()
  }
}

private[amqp] object ChannelWrapper extends ChannelFactory {
  def apply(channel: Channel, eventConsumer: Event ⇒ Unit) = new ChannelWrapper(channel, eventConsumer, ScheduledExecutor.defaultScheduledExecutor)
}
