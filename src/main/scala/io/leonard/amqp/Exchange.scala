package io.leonard.amqp

object Exchange {
  val Default = Exchange("")
  val Direct = Exchange("amq.direct")
  val Fanout = Exchange("amq.fanout")
  val Topic = Exchange("amq.topic")
  val Headers = Exchange("amq.headers")
  val Match = Exchange("amq.match")
}

/**
 * Describes an exchange which should already exist, an error will be thrown on use if it does not
 *
 * It is recommended to use ChannelOwner.declareExchange or ChannelOwner.declareExchangePassive to create this as they
 * ensures the exchange exists.
 */
case class Exchange(name: String) {
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

abstract class ExchangeType(val name: String)
object ExchangeType {
  case object direct extends ExchangeType("direct")
  case object topic extends ExchangeType("topic")
  case object fanout extends ExchangeType("fanout")
  case object headers extends ExchangeType("headers")
}
