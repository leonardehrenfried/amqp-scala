package io.relayr.amqp.rpc.client

import io.relayr.amqp._

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ Promise, Future }
import scala.concurrent.duration.FiniteDuration

/**
 * Sets up a responseQueue, controls the allocation of correlation ids, replyTo queue, fulfilling Promises, timing out responses, reconnecting to queues
 */
private[client] class ResponseDispatcher(channelOwner: ChannelOwner) {
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
    ResponseSpec(correlationId = correlationId, replyTo = replyQueueName, promise.future)
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
