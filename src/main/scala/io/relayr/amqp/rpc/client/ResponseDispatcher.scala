package io.relayr.amqp.rpc.client

import io.relayr.amqp._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * Sets up a responseQueue, controls the allocation of correlation ids, replyTo queue, fulfilling Promises, timing out responses
 */
private[client] class ResponseDispatcher(channelOwner: ChannelOwner) {
  val replyQueueName: String = channelOwner.createQueue(QueueDeclare(None)).name
  @volatile var callCounter: Long = 0L

  def prepareResponse(timeout: FiniteDuration): ResponseSpec =
    ResponseSpec(correlationId = nextCorrelationId, replyTo = replyQueueName, Future.successful(null))

  private def nextCorrelationId: String = {
    callCounter += 1
    callCounter.toString
  }
}

private[client] case class ResponseSpec(correlationId: String, replyTo: String, response: Future[Message])
