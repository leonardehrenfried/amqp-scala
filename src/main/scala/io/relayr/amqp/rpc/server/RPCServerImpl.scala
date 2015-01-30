package io.relayr.amqp.rpc.server

import io.relayr.amqp.DeliveryMode.NotPersistent
import io.relayr.amqp._
import io.relayr.amqp.properties.Key.{ CorrelationId, ReplyTo }

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
private[amqp] class RPCServerImpl(channelOwner: ChannelOwner, listenQueue: Queue, ackMode: RpcServerAutoAckMode, implicit val executionContext: ExecutionContext, handler: Message ⇒ Future[Message]) extends Closeable {
  private val responseExchange: ExchangePassive = Exchange.Default
  private val deliveryMode: NotPersistent.type = DeliveryMode.NotPersistent

  private val consumerCloser = channelOwner.addConsumerAckManual(listenQueue, requestConsumer)

  private def requestConsumer(request: Message, manualAcker: ManualAcker): Unit = {
    if (ackMode == RpcServerAutoAckMode.AckOnReceive)
      manualAcker.ack()
    executionContext.prepare().execute(new RPCRunnable(request, manualAcker))
  }

  private class RPCRunnable(request: Message, manualAcker: ManualAcker) extends Runnable {
    override def run(): Unit = {
      try {
        for {
          replyTo ← request.property(ReplyTo)
          correlationId ← request.property(CorrelationId)
        } for (result ← handler(request))
          onSuccessResponse(replyTo, correlationId, result)
        onHandled()
      } catch {
        case e: Exception ⇒ onHandleException(e)
      }
    }

    def onHandleException(e: Exception) =
      if (ackMode == RpcServerAutoAckMode.AckOnHandled || ackMode == RpcServerAutoAckMode.AckOnSuccessfulResponse)
        manualAcker.reject(requeue = false)

    def onSuccessResponse(replyTo: String, correlationId: String, result: Message) {
      if (ackMode == RpcServerAutoAckMode.AckOnSuccessfulResponse)
        manualAcker.ack()
      val responseRoute: RoutingDescriptor = responseExchange.route(replyTo, deliveryMode)
      channelOwner.send(responseRoute, result.withProperties(CorrelationId → correlationId))
    }

    def onHandled() =
      if (ackMode == RpcServerAutoAckMode.AckOnHandled)
        manualAcker.ack()
  }

  override def close(): Unit = consumerCloser.close()
}
