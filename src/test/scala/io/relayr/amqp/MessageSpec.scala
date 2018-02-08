package io.leonard.amqp

import java.nio.charset.Charset
import java.util.UUID

import io.leonard.amqp.properties.Key
import io.leonard.amqp.properties.Key.{ ContentEncoding, ContentType }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.language.implicitConversions

class MessageSpec extends FlatSpec with Matchers with MockFactory {

  "Messages" should "be constructed properly from json strings" in {
    val m = Message.JSONString("json")
    m.property(ContentEncoding) should be (Some("UTF-8"))
    m.property(ContentType) should be (Some("application/json"))
    val Message.JSONString(s) = m
    s should be ("json")
  }

  it should "be constructed properly from strings" in {
    val m = Message.String("string")
    m.property(ContentEncoding) should be (Some("UTF-8"))
    m.property(ContentType) should be (None)
    val Message.String(s) = m
    s should be ("string")
  }

  it should "be constructed properly from octet streams" in {
    val m = Message.OctetStream(Array(1: Byte, 2: Byte))
    m.property(ContentEncoding) should be (None)
    m.property(ContentType) should be (Some("application/octet-stream"))
    val Message.OctetStream(s) = m
    s should be (Array(1: Byte, 2: Byte))
  }

  it should "be constructed properly from arrays" in {
    val m = Message.Array(Array(1: Byte, 2: Byte))
    m.property(ContentEncoding) should be (None)
    m.property(ContentType) should be (None)
    val Message.Array(s) = m
    s should be (Array(1: Byte, 2: Byte))
  }

  it should "be constructed properly from its parts" in {
    val m = Message(MessageProperties(), ByteArray(1: Byte, 2: Byte))
    val Message(p, s) = m
    p should be (MessageProperties())
    s should be (ByteArray(1: Byte, 2: Byte))
  }

  it should "extract properties" in {
    val m = Message.String("string").withProperties(
      Key.AppId → "app id"
    )
    m.property(Key.AppId) should be (Some("app id"))
    m.property(Key.CorrelationId) should be (None)
  }

  it should "extract headers" in {
    val m = Message.String("string").withHeaders(
      "key" → "value"
    )
    m.header("key") should be (Some("value"))
    m.header("notkey") should be (None)
  }

  they should "toString body to string if encoding is set" in {
    val m = Message.String("string")
    m.toString should be ("""Message(MessageProperties(ContentEncoding -> UTF-8), body="string")""")
  }

  they should "toString body no output with no encoding" in {
    val m = Message.Array("string".getBytes(Charset.defaultCharset()))
    m.toString should be ("""Message(MessageProperties(), body=6 BYTES)""")
  }

  "Message creation" should "fail fast on attempt to add unsupported value type" in {
    val message = Message.String("")
    intercept[IllegalArgumentException](message.withHeaders("id" → UUID.randomUUID()))
  }

  "Message conversion" should "work in the basic case" in {
    implicit val converter = new ToMessage[String] {
      def convert(value: String) = Message.String(value.toString).withProperties(ContentType → "text/plain")
    }

    val Message.String(body) = Message("hello")

    body should be ("hello")
    Message("hello").property(ContentType) should be (Some("text/plain"))
  }

  it should "work in the more complicated case" in {
    implicit def converter[N](implicit numeric: Numeric[N]): ToMessage[N] = new ToMessage[N] {
      def convert(value: N) = Message.String(numeric.getClass + " " + value.toString).withProperties(ContentType → "number")
    }

    val Message.String(body) = Message(3)

    body should be ("class scala.math.Numeric$IntIsIntegral$ 3")
    Message(3).property(ContentType) should be (Some("number"))
  }
}
