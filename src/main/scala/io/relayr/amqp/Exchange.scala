package io.relayr.amqp

object Exchange {
  val Default = ExchangePassive("")
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
  def route(routingKey: String, deliveryMode: DeliveryMode, mandatory: Boolean = false, immediate: Boolean = false) =
    RoutingDescriptor(this, routingKey, Some(deliveryMode), mandatory = mandatory, immediate = immediate)

  /**
   * Specifies routing and send parameters
   * @param routingKey the exchange uses this to decide which queue(s) the message is to be added to
   * @param mandatory a message with this flag will be returned by the exchange if it finds that no queues match the routingKey
   * @param immediate a message with this flag will only be delivered if a matching queue has a ready consumer, if not it is returned
   */
  def route(routingKey: String, mandatory: Boolean, immediate: Boolean) =
    RoutingDescriptor(this, routingKey, None, mandatory = mandatory, immediate = immediate)

  /**
   * Specifies routing parameters
   * @param routingKey the exchange uses this to decide which queue(s) the message is to be added to
   */
  def route(routingKey: String): RoutingDescriptor =
    route(routingKey, mandatory = false, immediate = false)

  /**
   * Specifies routing and send parameters
   * @param routingKey the exchange uses this to decide which queue(s) the message is to be added to
   * @param deliveryMode defines whether a message should be persisted if the queue it is on is persisted
   * @param mandatory a message with this flag will be returned by the exchange if it finds that no queues match the routingKey
   * @param immediate a message with this flag will only be delivered if a matching queue has a ready consumer, if not it is returned
   */
  def route(routingKey: String, mandatory: Boolean, immediate: Boolean, deliveryMode: Option[DeliveryMode]) =
    RoutingDescriptor(this, routingKey, deliveryMode, mandatory = mandatory, immediate = immediate)
}

/** Parameters to create a new exchange */
case class ExchangeDeclare(name: String, exchangeType: String, durable: Boolean = false, autoDelete: Boolean = false, args: Map[String, AnyRef] = Map.empty) extends Exchange
