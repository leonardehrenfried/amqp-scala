package io.relayr.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AMQP.BasicProperties

/** Message blob with content headers */
case class Message(contentType: String, contentEncoding: String, body: ByteArray)

case class Publish(routingDescriptor: RoutingDescriptor, message: Message, properties: AMQP.BasicProperties = new BasicProperties())

case class RoutingDescriptor(exchange: ExchangePassive, routingKey: String, deliveryMode: DeliveryMode)
