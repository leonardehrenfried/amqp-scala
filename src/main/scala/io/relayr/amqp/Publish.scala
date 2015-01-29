package io.relayr.amqp

case class Publish(routingDescriptor: RoutingDescriptor, message: Message)

case class RoutingDescriptor(exchange: ExchangePassive, routingKey: String, deliveryMode: DeliveryMode)
