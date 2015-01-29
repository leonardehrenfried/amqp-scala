package io.relayr.amqp

import java.util.Date

import com.rabbitmq.client.AMQP.BasicProperties

import scala.collection.JavaConversions

package object properties {
  sealed abstract class Key[V](private val bs: BasicProperties.Builder ⇒ V ⇒ BasicProperties.Builder) {
    def builderSetter(builder: BasicProperties.Builder)(value: Any) =
      bs(builder)(value.asInstanceOf[V])
  }

  sealed abstract class ConvertKey[V, R](bs: BasicProperties.Builder ⇒ V ⇒ BasicProperties.Builder, in: R ⇒ V, out: V ⇒ R) extends Key[V](bs) {
    override def builderSetter(builder: BasicProperties.Builder)(value: Any) =
      bs(builder)(in(value.asInstanceOf[R]))
  }

  object Key {
    object ContentType extends Key(_.contentType)
    object ContentEncoding extends Key(_.contentEncoding)
    object Type extends Key(_.`type`)
    object Timestamp extends ConvertKey[Date, Date](_.timestamp, _.clone().asInstanceOf[Date], _.clone().asInstanceOf[Date]) ///
    object MessageId extends Key(_.messageId)
    object ReplyTo extends Key(_.replyTo)
    object DeliveryMode extends Key(_.deliveryMode)
    object UserId extends Key(_.userId)
    object Expiration extends Key(_.expiration)
    object Priority extends Key(_.priority)
    object Headers extends ConvertKey[java.util.Map[String, AnyRef], Map[String, AnyRef]](_.headers, JavaConversions.mapAsJavaMap, JavaConversions.mapAsScalaMap[String, AnyRef] _ andThen (_.toMap)) //
    object CorrelationId extends Key(_.correlationId)
    object AppId extends Key(_.appId)
  }
}
