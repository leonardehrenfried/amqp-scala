package io.relayr.amqp

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.AMQP.BasicProperties.Builder
import io.relayr.amqp.properties.Key

class MessageProperties(props: Map[Key[_, _], Any]) {
  def getOrNull[J, V](key: Key[J, V]): J = {
    val option: Option[J] = props.get(key).map(v ⇒ key.in(v.asInstanceOf[V]))
    option.getOrElse(null).asInstanceOf[J]
  }

  def toBasicProperties: BasicProperties = {
    val builder = new Builder()
    props.foreach {
      case (key, value) ⇒
        key.builderSetter(builder)(value)
    }
    builder.build()
  }
}

object MessageProperties {
  def apply(elems: (Key[_, _], Any)*): MessageProperties = new MessageProperties((Map.newBuilder[Key[_, _], Any] ++= elems).result())
}
