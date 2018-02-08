package io.leonard.amqp.connection

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.ReturnListener
import io.leonard.amqp.concurrent.ScheduledExecutor

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration

/**
 * Handles tracking of message returns for when the sender needs to know about returns.
 * @param scheduledExecutor to expire the waiting for a return
 */
private class ReturnedMessageHandler(scheduledExecutor: ScheduledExecutor) {
  @volatile private var messageCounter: Long = 0L
  private val returnCallbackMap = TrieMap[String, () ⇒ Unit]()

  def newReturnListener() = new ReturnListener {
    override def handleReturn(replyCode: Int, replyText: String, exchange: String, routingKey: String, properties: BasicProperties, body: Array[Byte]): Unit = {
      for {
        messageId ← Option(properties.getMessageId)
        callback ← returnCallbackMap.remove(messageId)
      } callback()
    }
  }

  def setupReturnCallback(onReturn: () ⇒ Unit, timeout: FiniteDuration): String = {
    val messageId: String = nextMessageId
    returnCallbackMap += (messageId → onReturn)
    val timeoutFuture = scheduledExecutor.delayExecution {
      returnCallbackMap.remove(messageId)
    }(timeout)
    messageId
  }

  private def nextMessageId: String = {
    messageCounter += 1
    messageCounter.toString
  }
}
