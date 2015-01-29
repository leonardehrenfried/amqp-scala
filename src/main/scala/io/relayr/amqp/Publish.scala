package io.relayr.amqp

import com.rabbitmq.client.AMQP

case class Publish(routingDescriptor: RoutingDescriptor, message: Message, properties: AMQP.BasicProperties = new AMQP.BasicProperties())

case class RoutingDescriptor(exchange: ExchangePassive, routingKey: String, deliveryMode: DeliveryMode)
