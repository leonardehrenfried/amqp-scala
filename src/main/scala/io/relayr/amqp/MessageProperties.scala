package io.relayr.amqp

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.AMQP.BasicProperties.Builder
import io.relayr.amqp.properties.Key
import io.relayr.amqp.properties.Key._

class MessageProperties(private val props: Map[Key[_, _], Any]) {

  def get[V](key: Key[_, V]): Option[V] =
    props.get(key).map(_.asInstanceOf[V])

  def getOrNull[J, V](key: Key[J, V]): J = {
    val option: Option[V] = get(key)
    val map: Option[J] = option.map(key.in)
    map.getOrElse(null.asInstanceOf[J])
  }

  def toBasicProperties: BasicProperties = {
    val builder = new Builder()
    props.foreach {
      case (key, value) ⇒
        key.builderSetter(builder)(value)
    }
    builder.build()
  }

  def ++(messageProperties: MessageProperties): MessageProperties =
    new MessageProperties(props ++ messageProperties.props)

  def ++(elems: (Key[_, _], Any)*): MessageProperties =
    new MessageProperties(props ++ elems)

  def +(elems: (Key[_, _], Any)): MessageProperties =
    new MessageProperties(props + elems)

  override def toString: String = "MessageProperties(" + props.toString + ")"

  override def equals(other: Any): Boolean = other match {
    case that: MessageProperties ⇒
      props == that.props
    case _ ⇒ false
  }

  override def hashCode(): Int = props.hashCode()
}

object MessageProperties {

  def apply(elems: (Key[_, _], Any)*): MessageProperties =
    new MessageProperties((Map.newBuilder[Key[_, _], Any] ++= elems.filterNot(_._2 == null)).result())

  def apply(bp: BasicProperties): MessageProperties = MessageProperties(
    ContentType -&> bp.getContentType,
    ContentEncoding -&> bp.getContentEncoding,
    Type -&> bp.getType,
    Timestamp -&> bp.getTimestamp,
    MessageId -&> bp.getMessageId,
    ReplyTo -&> bp.getReplyTo,
    Key.DeliveryMode -&> bp.getDeliveryMode,
    UserId -&> bp.getUserId,
    Expiration -&> bp.getExpiration,
    Priority -&> bp.getPriority,
    Headers -&> bp.getHeaders,
    CorrelationId -&> bp.getCorrelationId,
    AppId -&> bp.getAppId
  )
}

