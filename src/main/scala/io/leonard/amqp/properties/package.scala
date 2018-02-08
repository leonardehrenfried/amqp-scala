package io.leonard.amqp

import java.util.Date

import com.rabbitmq.client.AMQP.BasicProperties

import scala.collection.JavaConverters._

package object properties {

  sealed abstract class Key[J, V](bs: BasicProperties.Builder ⇒ J ⇒ BasicProperties.Builder, val in: V ⇒ J, val out: J ⇒ V) {
    def convert(value: J): V =
      if (value == null)
        null.asInstanceOf[V]
      else
        out(value)

    def builderSetter(builder: BasicProperties.Builder)(value: Any) =
      bs(builder)(in(value.asInstanceOf[V]))

    def unapply(messageProperties: MessageProperties) =
      messageProperties.get(this)

    /**
     * Associates a key to it's value, this uses domain type from the scala library (type-checked).
     * To use the domain type of the java library use `-&>` to also perform the automatic conversion
     */
    @inline def ->(y: V): (Key[J, V], V) = (this, y)
    def →(y: V) = ->(y)

    /**
     * Associates a key to it's value, this uses domain type from the java library (type-checked).
     * To use the domain type of the scala library use `->`
     */
    @inline def -&>(y: J): (Key[J, V], V) = (this, convert(y))

    override def toString: String = getClass.getName.split('$')(2)
  }

  sealed abstract class BasicKey[V](bs: BasicProperties.Builder ⇒ V ⇒ BasicProperties.Builder)
    extends Key[V, V](bs, a ⇒ a, a ⇒ a)

  /**
   * Keys for Message properties
   */
  object Key {
    case object ContentType extends BasicKey(_.contentType)
    case object ContentEncoding extends BasicKey(_.contentEncoding)
    case object Type extends BasicKey(_.`type`)
    case object Timestamp extends Key[Date, Date](_.timestamp, _.clone().asInstanceOf[Date], _.clone().asInstanceOf[Date]) ///
    case object MessageId extends BasicKey(_.messageId)
    case object ReplyTo extends BasicKey(_.replyTo)
    case object DeliveryMode extends Key[Integer, DeliveryMode](_.deliveryMode, _.value, i ⇒ io.leonard.amqp.DeliveryMode.apply(i.intValue))
    case object UserId extends BasicKey(_.userId)
    case object Expiration extends BasicKey(_.expiration)
    case object Priority extends BasicKey(_.priority)
    case object Headers extends Key[java.util.Map[String, AnyRef], Map[String, AnyRef]](_.headers, _.asJava, _.asScala.toMap) //
    case object CorrelationId extends BasicKey(_.correlationId)
    case object AppId extends BasicKey(_.appId)
  }
}
