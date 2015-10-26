package io.relayr.amqp

/**
 * The message and any routing information that is included when it is received from a queue.
 * @param exchange
 * @param routingKey
 * @param message
 */
case class Envelope(exchange: String, routingKey: String, message: Message)
