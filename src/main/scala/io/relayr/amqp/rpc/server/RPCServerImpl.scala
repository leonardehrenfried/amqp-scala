package io.relayr.amqp.rpc.server

import com.rabbitmq.client.AMQP.BasicProperties.Builder
import io.relayr.amqp.DeliveryMode.NotPersistent
import io.relayr.amqp._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Manages consuming from a queue, passing the messages to a handler, and returning the responses to the replyTo address
 * @param channelOwner used both for listening and for sending replies
 * @param listenQueue description of queue to receive requests on
 * @param executionContext to run the handler
 * @param handler handles incoming messages
 */
private[amqp] class RPCServerImpl(channelOwner: ChannelOwner, listenQueue: Queue, implicit val executionContext: ExecutionContext, handler: Message ⇒ Future[Message]) extends RPCServer {
  val deliveryMode: NotPersistent.type = DeliveryMode.NotPersistent

  channelOwner.addConsumer(listenQueue, autoAck = true, requestConsumer)

  private def requestConsumer(request: Delivery): Unit =
    executionContext.prepare().execute(new RPCRunnable(request))

  private class RPCRunnable(request: Delivery) extends Runnable {
    override def run(): Unit = {
      for (result ← handler(request.message)) {
        channelOwner.send(Exchange.Direct.route(request.replyTo, deliveryMode), result, new Builder().correlationId(request.correlationId).build())
      }
    }
  }

  override def close(): Unit = ???
}
