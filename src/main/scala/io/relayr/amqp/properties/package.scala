package io.relayr.amqp

import java.util.Date

import com.rabbitmq.client.AMQP.BasicProperties

import scala.collection.JavaConversions

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

    override def toString: String = getClass.getName.split('$')(2)
  }

  sealed abstract class BasicKey[V](bs: BasicProperties.Builder ⇒ V ⇒ BasicProperties.Builder)
    extends Key[V, V](bs, a ⇒ a, a ⇒ a)

  object Key {
    object ContentType extends BasicKey(_.contentType)
    object ContentEncoding extends BasicKey(_.contentEncoding)
    object Type extends BasicKey(_.`type`)
    object Timestamp extends Key[Date, Date](_.timestamp, _.clone().asInstanceOf[Date], _.clone().asInstanceOf[Date]) ///
    object MessageId extends BasicKey(_.messageId)
    object ReplyTo extends BasicKey(_.replyTo)
    object DeliveryMode extends Key[Integer, DeliveryMode](_.deliveryMode, _.value, i ⇒ io.relayr.amqp.DeliveryMode.apply(i.intValue))
    object UserId extends BasicKey(_.userId)
    object Expiration extends BasicKey(_.expiration)
    object Priority extends BasicKey(_.priority)
    object Headers extends Key[java.util.Map[String, AnyRef], Map[String, AnyRef]](_.headers, JavaConversions.mapAsJavaMap, JavaConversions.mapAsScalaMap[String, AnyRef] _ andThen (_.toMap)) //
    object CorrelationId extends BasicKey(_.correlationId)
    object AppId extends BasicKey(_.appId)
  }
}
