package io.relayr.amqp

import java.nio.charset.Charset

import io.relayr.amqp.properties.Key.{ ContentEncoding, ContentType }

object Message {
  private val utf8 = "UTF-8"

  def apply(messageProperties: MessageProperties, body: ByteArray) =
    new Message(messageProperties, body)

  def unapply(message: Message): Option[(String, String, ByteArray)] = for {
    contentType ← message.messageProperties.get(ContentType)
    contentEncoding ← message.messageProperties.get(ContentEncoding)
  } yield (contentType, contentEncoding, message.body)

  object JSONString {
    def apply(string: String): Message =
      new Message(MessageProperties(ContentType → "application/json", ContentEncoding → utf8), ByteArray(string, Charset.forName(utf8)))

    def unapply(message: Message): Option[String] = for {
      contentType ← message.messageProperties.get(ContentType) if contentType equals "application/json"
      contentEncoding ← message.messageProperties.get(ContentEncoding)
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
      contentEncoding ← message.messageProperties.get(ContentEncoding)
    } yield message.body.decodeString(Charset.forName(contentEncoding))
  }
}

/** Message blob with content headers */
class Message(val messageProperties: MessageProperties, val body: ByteArray) {
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
