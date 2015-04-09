package io.relayr.amqp

case class Publish(routingDescriptor: RoutingDescriptor, message: Message)

/**
 * Specifies routing and send parameters
 * @param exchange for the message to be sent to
 * @param routingKey the exchange uses this to decide which queue(s) the message is to be added to
 * @param deliveryMode defines whether a message should be persisted if the queue it is on is persisted
 * @param mandatory a message with this flag will be returned by the exchange if it finds that no queues match the routingKey
 * @param immediate a message with this flag will only be delivered if a matching queue has a ready consumer, if not it is returned
 */
case class RoutingDescriptor(exchange: Exchange, routingKey: String, deliveryMode: Option[DeliveryMode], mandatory: Boolean, immediate: Boolean)
