package io.relayr.amqp.rpc.client

import io.relayr.amqp._
import io.relayr.amqp.concurrent.{ CancellableFuture, ScheduledExecutor }

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ Promise, Future }
import scala.concurrent.duration.FiniteDuration

/**
 * Sets up a responseQueue, controls the allocation of correlation ids, replyTo queue, fulfilling Promises, timing out responses, reconnecting to replyTo queues
 */
private[client] class ResponseDispatcher(channelOwner: ChannelOwner, scheduledExecutor: ScheduledExecutor) {
  implicit val executionContext = scheduledExecutor.executionContext
  val replyQueueName: String = channelOwner.createQueue(QueueDeclare(None)).name
  @volatile var callCounter: Long = 0L
  val correlationMap = TrieMap[String, Promise[Message]]()

  private def consumer(delivery: Delivery): Unit =
    correlationMap.remove(delivery.correlationId) match {
      case Some(promise) ⇒ promise.success(delivery.message)
      case None          ⇒ // TODO probably just create an event
    }

  channelOwner.addConsumer(replyQueueName, autoAck = false, consumer)

  def prepareResponse(timeout: FiniteDuration): ResponseSpec = {
    val correlationId: String = nextUniqueCorrelationId
    val promise: Promise[Message] = Promise()
    correlationMap += (correlationId → promise)
    val timeoutFuture: CancellableFuture[Message] = scheduledExecutor.delayExecution(throw RPCTimeout())(timeout)
    promise.future.foreach(_ ⇒ timeoutFuture.cancel(mayInterruptIfRunning = false))
    ResponseSpec(correlationId = correlationId, replyTo = replyQueueName, Future.firstCompletedOf(Seq(promise.future, timeoutFuture: Future[Message])))
  }

  private def nextUniqueCorrelationId: String = {
    callCounter += 1
    callCounter.toString
  }

  /**
   * The number of RPC's awaiting responses
   */
  def countAwaiting =
    correlationMap.size
}

private[client] case class ResponseSpec(correlationId: String, replyTo: String, response: Future[Message])

trait Delivery {
  def message: Message = ???
  def correlationId: String
}

case class RPCTimeout() extends Throwable