package io.relayr.amqp.rpc.server

import com.rabbitmq.client.AMQP.BasicProperties.Builder
import io.relayr.amqp.DeliveryMode.NotPersistent
import io.relayr.amqp._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Manages consuming from a queue, passing the messages to a handler, and returning the responses to the replyTo address.
 *
 * Acks: the request is acknowledged once the consumer thread has successfully returned (not when the future completes), an Exception will cause a reject but the message will not be requeued
 *
 * TODO: add configuration of ack strategy
 *
 * @param channelOwner used both for listening and for sending replies
 * @param listenQueue description of queue to receive requests on
 * @param executionContext to run the handler
 * @param handler handles incoming messages
 */
private[amqp] class RPCServerImpl(channelOwner: ChannelOwner, listenQueue: Queue, implicit val executionContext: ExecutionContext, handler: Message ⇒ Future[Message]) extends Closeable {
  private val responseExchange: ExchangePassive = Exchange.Default
  private val deliveryMode: NotPersistent.type = DeliveryMode.NotPersistent

  private val consumerCloser = channelOwner.addConsumerAckManual(listenQueue, requestConsumer)

  private def requestConsumer(request: Delivery, manualAcker: ManualAcker): Unit =
    executionContext.prepare().execute(new RPCRunnable(request, manualAcker))

  private class RPCRunnable(request: Delivery, manualAcker: ManualAcker) extends Runnable {
    override def run(): Unit = {
      try {
        for (result ← handler(request.message)) {
          val responseRoute: RoutingDescriptor = responseExchange.route(request.replyTo, deliveryMode)
          channelOwner.send(responseRoute, result, new Builder().correlationId(request.correlationId).build())
        }
        manualAcker.ack()
      } catch {
        case e: Exception ⇒ manualAcker.reject(requeue = false)
      }
    }
  }

  override def close(): Unit = consumerCloser.close()
}
