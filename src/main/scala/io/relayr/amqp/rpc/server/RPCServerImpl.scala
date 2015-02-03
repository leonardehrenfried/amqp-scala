package io.relayr.amqp.rpc.server

import io.relayr.amqp.Event.HandlerError
import io.relayr.amqp._
import io.relayr.amqp.properties.Key.{ CorrelationId, ReplyTo }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

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
private[amqp] class RPCServerImpl(channelOwner: ChannelOwner, listenQueue: Queue, ackMode: RpcServerAutoAckMode, eventConsumer: Event ⇒ Unit, implicit val executionContext: ExecutionContext, handler: Message ⇒ Future[Message], responseParameters: ResponseParameters) extends Closeable {
  private val responseExchange: ExchangePassive = Exchange.Default

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
        } handler(request).andThen {
          case Success(result) ⇒ onSuccessResponse(replyTo, correlationId, result)
          case Failure(e)      ⇒ onFutureFailure(e)
        }
        onHandled()
      } catch {
        case e: Exception ⇒ onHandleException(e)
      }
    }

    def onFutureFailure(e: Throwable) = {
      eventConsumer(HandlerError(e))
      if (ackMode == RpcServerAutoAckMode.AckOnSuccessfulResponse)
        manualAcker.reject(requeue = false)
    }

    def onHandleException(e: Throwable) = {
      eventConsumer(HandlerError(e))
      if (ackMode == RpcServerAutoAckMode.AckOnHandled || ackMode == RpcServerAutoAckMode.AckOnSuccessfulResponse)
        manualAcker.reject(requeue = false)
    }

    def onSuccessResponse(replyTo: String, correlationId: String, result: Message) {
      if (ackMode == RpcServerAutoAckMode.AckOnSuccessfulResponse)
        manualAcker.ack()
      val responseRoute: RoutingDescriptor = responseExchange.route(replyTo, mandatory = responseParameters.mandatory, immediate = responseParameters.immediate, deliveryMode = responseParameters.deliveryMode)
      channelOwner.send(responseRoute, result.withProperties(CorrelationId → correlationId))
    }

    def onHandled() =
      if (ackMode == RpcServerAutoAckMode.AckOnHandled)
        manualAcker.ack()
  }

  override def close(): Unit = consumerCloser.close()
}

/** The parts of the RoutingDescriptor which are not controlled by the request or server */
case class ResponseParameters(mandatory: Boolean, immediate: Boolean, deliveryMode: Option[DeliveryMode])
