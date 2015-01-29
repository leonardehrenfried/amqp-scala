package io.relayr.amqp.rpc.client

import io.relayr.amqp._
import io.relayr.amqp.concurrent.{ CancellableFuture, ScheduledExecutor }
import io.relayr.amqp.properties.Key.CorrelationId

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
  @volatile private var callCounter: Long = 0L
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
    val timeoutFuture: CancellableFuture[Message] = scheduledExecutor.delayExecution(throw RPCTimeout())(timeout)
    promise.future.foreach(_ ⇒ timeoutFuture.cancel(mayInterruptIfRunning = false))
    ResponseSpec(correlationId = correlationId, replyTo = replyQueueName,
      Future.firstCompletedOf(Seq(promise.future, timeoutFuture: Future[Message])))
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

case class RPCTimeout() extends Throwable