package io.leonard.amqp.rpc.client

import java.util.concurrent.atomic.AtomicLong

import io.leonard.amqp._
import io.leonard.amqp.concurrent.{ CancellableFuture, ScheduledExecutor }
import io.leonard.amqp.properties.Key.CorrelationId

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ Promise, Future }
import scala.concurrent.duration.FiniteDuration

private[client] trait ResponseController {
  def prepareResponse(timeout: FiniteDuration): ResponseSpec
}

/**
 * Sets up a responseQueue, controls the allocation of correlation ids, replyTo queue, fulfilling Promises, timing out responses, reconnecting to replyTo queues
 */
private[amqp] class ResponseDispatcher(listenChannel: ChannelOwner, scheduledExecutor: ScheduledExecutor) extends ResponseController {
  private implicit val executionContext = scheduledExecutor.executionContext
  val replyQueueName: String = listenChannel.declareQueue(QueueDeclare(None))
  private val callCounter = new AtomicLong(0)
  private val correlationMap = TrieMap[String, Promise[Message]]()

  private def consumer(message: Message): Unit =
    message.property(CorrelationId) match {
      case Some(correlationId) ⇒
        correlationMap.remove(correlationId) match {
          case Some(promise) ⇒ promise.success(message)
          case None          ⇒ // TODO probably just create an event
        }
      case None ⇒
    }

  listenChannel.addConsumer(QueuePassive(replyQueueName), consumer)

  override def prepareResponse(timeout: FiniteDuration): ResponseSpec = {
    val correlationId: String = nextUniqueCorrelationId
    val promise: Promise[Message] = Promise()
    correlationMap += (correlationId → promise)
    val timeoutFuture: CancellableFuture[Message] = scheduledExecutor.delayExecution {
      correlationMap.remove(correlationId)
      throw RPCTimeout()
    }(timeout)
    promise.future.foreach(_ ⇒ timeoutFuture.cancel(mayInterruptIfRunning = false))
    ResponseSpec(correlationId = correlationId, replyTo = replyQueueName,
      Future.firstCompletedOf(Seq(promise.future, timeoutFuture: Future[Message])),
      () ⇒ promise.failure(new UndeliveredException))
  }

  private def nextUniqueCorrelationId: String = callCounter.incrementAndGet().toString

  /**
   * The number of RPC's awaiting responses
   */
  def countAwaiting =
    correlationMap.size
}

private[client] case class ResponseSpec(correlationId: String, replyTo: String, response: Future[Message], onReturn: () ⇒ Unit)

case class RPCTimeout() extends Throwable