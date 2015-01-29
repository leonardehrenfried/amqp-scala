package io.relayr.amqp

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.AMQP.BasicProperties.Builder
import io.relayr.amqp.properties.Key

class MessageProperties(props: Map[Key[_], Any]) {
  def getOrNull[V](key: Key[V]): V = ???

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
  def apply(elems: (Key[_], Any)*): MessageProperties = new MessageProperties((Map.newBuilder[Key[_], Any] ++= elems).result())
}
