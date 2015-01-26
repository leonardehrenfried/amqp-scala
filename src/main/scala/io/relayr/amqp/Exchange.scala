package io.relayr.amqp

object Exchange {
  val Direct = ExchangePassive("amq.direct")
  val Fanout = ExchangePassive("amq.fanout")
  val Topic = ExchangePassive("amq.topic")
  val Headers = ExchangePassive("amq.headers")
  val Match = ExchangePassive("amq.match")
}

/** Defines an exchange to connect to or create */
sealed trait Exchange

/** Describes an exchange which should already exist, an error is thrown if it does not */
case class ExchangePassive(name: String) extends Exchange {
  def route(routingKey: String, deliveryMode: DeliveryMode) =
    RoutingDescriptor(this, routingKey, deliveryMode)
}

/** Parameters to create a new exchange */
case class ExchangeDeclare(name: String, exchangeType: String, durable: Boolean = false, autoDelete: Boolean = false, args: Map[String, AnyRef] = Map.empty) extends Exchange
