package io.relayr.amqp

case class Publish(routingDescriptor: RoutingDescriptor, message: Message)

case class RoutingDescriptor(exchange: ExchangePassive, routingKey: String, deliveryMode: Option[DeliveryMode] = None, mandatory: Boolean = false, immediate: Boolean = false)
