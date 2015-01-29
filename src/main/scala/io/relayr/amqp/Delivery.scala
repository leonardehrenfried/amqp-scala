package io.relayr.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AMQP.BasicProperties

// TODO this is covered in the properties now
trait Delivery {
  def replyTo: String
  def message: Message
  def correlationId: String
}

object Delivery {
  def apply(properties: AMQP.BasicProperties, body: Array[Byte]): Delivery =
    DeliveryImpl(buildMessage(properties, body), properties.getCorrelationId, properties.getReplyTo)

  private def buildMessage(properties: BasicProperties, body: Array[Byte]): Message = {
    Message(MessageProperties(properties), ByteArray(body))
  }

  private case class DeliveryImpl(message: Message, correlationId: String, replyTo: String) extends Delivery
}

