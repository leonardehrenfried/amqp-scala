package io.relayr.amqp

import java.nio.charset.Charset

import com.rabbitmq.client.AMQP
import io.relayr.amqp.properties.Key
import io.relayr.amqp.properties.Key.{ ContentEncoding, ContentType }

object Message {
  private val utf8 = "UTF-8"

  def apply(messageProperties: MessageProperties, body: ByteArray) =
    new Message(messageProperties, body)

  def apply(properties: AMQP.BasicProperties, body: Array[Byte]): Message =
    Message(MessageProperties(properties), ByteArray(body))

  def unapply(message: Message): Option[(String, String, ByteArray)] = for {
    contentType ← message.property(ContentType)
    contentEncoding ← message.property(ContentEncoding)
  } yield (contentType, contentEncoding, message.body)

  object JSONString {
    def apply(string: String): Message =
      new Message(MessageProperties(ContentType → "application/json", ContentEncoding → utf8), ByteArray(string, Charset.forName(utf8)))

    def unapply(message: Message): Option[String] = for {
      contentType ← message.property(ContentType) if contentType equals "application/json"
      contentEncoding ← message.property(ContentEncoding)
    } yield message.body.decodeString(Charset.forName(contentEncoding))
  }

  object Array {
    def unapply(message: Message): Option[Array[Byte]] =
      Some(message.body.toArray)
  }

  object String {
    def apply(string: String): Message =
      new Message(MessageProperties(ContentEncoding → utf8), ByteArray(string, Charset.forName(utf8)))

    def unapply(message: Message): Option[String] = for {
      contentEncoding ← message.property(ContentEncoding)
    } yield message.body.decodeString(Charset.forName(contentEncoding))
  }

  object Raw {
    def unapply(message: Message): Option[(Array[Byte], AMQP.BasicProperties)] =
      Some(message.body.toArray, message.messageProperties.toBasicProperties)
  }

}

/** Message blob with content headers */
class Message(val messageProperties: MessageProperties, val body: ByteArray) {
  def withProperties(elems: (Key[_, _], Any)*) = new Message(messageProperties ++ (elems: _*), body)

  def property[V](key: properties.Key[_, V]) = messageProperties.get(key)
  def header(key: String): Option[AnyRef] = property(properties.Key.Headers).map(_(key))

  override def toString: String = s"Message($messageProperties, ${body.toString()})"

  def canEqual(other: Any): Boolean = other.isInstanceOf[Message]

  override def equals(other: Any): Boolean = other match {
    case that: Message ⇒
      (that canEqual this) &&
        messageProperties == that.messageProperties &&
        body == that.body
    case _ ⇒ false
  }

  override def hashCode(): Int = {
    val state = Seq(messageProperties, body)
    state.map(_.hashCode()).foldLeft(0)((a, b) ⇒ 31 * a + b)
  }
}
