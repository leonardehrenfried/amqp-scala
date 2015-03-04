package io.relayr.amqp

import java.nio.charset.Charset

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.impl.Frame
import io.relayr.amqp.properties.Key
import io.relayr.amqp.properties.Key.{ ContentEncoding, ContentType }

import scala.collection.JavaConversions

/** Message blob with content headers, usually you would use the child objects to construct / extract different types of messages */
object Message {
  private val utf8 = "UTF-8"

  def apply(messageProperties: MessageProperties, body: ByteArray) =
    new Message(messageProperties, body)

  def unapply(message: Message): Option[(MessageProperties, ByteArray)] =
    Some((message.messageProperties, message.body))

  /** Constructor / extractor for JSON messages, content-type is set to "application/json" and charset is defaulted to UTF-8 */
  object JSONString {
    def apply(string: String): Message =
      new Message(MessageProperties(
        ContentType -> "application/json",
        ContentEncoding → utf8
      ), ByteArray(string, Charset.forName(utf8)))

    def unapply(message: Message): Option[String] = for {
      contentType ← message.property(ContentType) if contentType equals "application/json"
      contentEncoding ← message.property(ContentEncoding)
    } yield message.body.decodeString(Charset.forName(contentEncoding))
  }

  /** Constructor / extractor for plain bytes, the same as Array, but content-type is set to "application/octet-stream" */
  object OctetStream {
    def apply(array: Array[Byte]) =
      new Message(MessageProperties(
        ContentType → "application/octet-stream"
      ), ByteArray(array))

    def unapply(message: Message): Option[Array[Byte]] = for {
      contentType ← message.property(ContentType) if contentType equals "application/octet-stream"
    } yield message.body.toArray
  }

  /** Constructor / extractor for plain bytes, with no properties set */
  object Array {
    def apply(array: Array[Byte]) =
      new Message(MessageProperties(), ByteArray(array))

    def unapply(message: Message): Option[Array[Byte]] =
      Some(message.body.toArray)
  }

  /** Constructor / extractor for String,  no content-type is set and charset is defaulted to UTF-8 */
  object String {
    def apply(string: String): Message =
      new Message(MessageProperties(ContentEncoding → utf8), ByteArray(string, Charset.forName(utf8)))

    def unapply(message: Message): Option[String] = for {
      contentEncoding ← message.property(ContentEncoding)
    } yield message.body.decodeString(Charset.forName(contentEncoding))
  }

  /** Constructor / extractor for working directly with the fields of the underlying java client */
  object Raw {
    def apply(body: Array[Byte], properties: AMQP.BasicProperties): Message =
      Message(MessageProperties(properties), ByteArray(body))

    def unapply(message: Message): Option[(Array[Byte], AMQP.BasicProperties)] =
      Some(message.body.toArray, message.messageProperties.toBasicProperties)
  }

}

/** Message blob with content headers */
class Message(val messageProperties: MessageProperties, val body: ByteArray) {
  /**
   * Creates a new Message with additional headers.
   *
   * Supported types are Strings, numbers, arrays, java maps, and some other types as supported by com.rabbitmq.client.impl.Frame#fieldValueSize(java.lang.Object)
   */
  @throws[IllegalArgumentException]("On unsupported value type")
  def withHeaders(elems: (String, AnyRef)*) = {
    Frame.arraySize(JavaConversions.seqAsJavaList(elems.map(_._2)))
    withProperties(properties.Key.Headers → (headers ++ elems))
  }

  /** Creates a new Message with additional properties */
  def withProperties(elems: (Key[_, _], Any)*) = new Message(messageProperties ++ (elems: _*), body)
  /** Creates a new Message with additional properties */
  def withProperties(elems: (Key[_, _], Any)) = new Message(messageProperties + elems, body)

  /**
   * Get a property value from the message
   * @tparam V type of value
   */
  def property[V](key: properties.Key[_, V]) = messageProperties.get(key)

  /**
   * Get a header value from the message
   */
  def header(key: String): Option[AnyRef] = headers.get(key)

  def headers: Map[String, AnyRef] = property(properties.Key.Headers).getOrElse(Map.empty)

  private def bodyAsString: Option[String] = property(ContentEncoding).map(Charset.forName).map(body.decodeString)

  override def toString: String = {
    val bodyToString = bodyAsString.map('"' + _ + '"').getOrElse(body.length + " BYTES")
    s"Message($messageProperties, body=$bodyToString)"
  }

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
