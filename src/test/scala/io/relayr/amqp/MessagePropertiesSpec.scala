package io.leonard.amqp

import java.util.Date

import io.leonard.amqp.DeliveryMode.Persistent
import io.leonard.amqp.properties.Key
import io.leonard.amqp.properties.Key._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class MessagePropertiesSpec extends FlatSpec with Matchers with MockFactory {

  "MessageProperties" should "to convertible to a AMQP.BasicProperties" in {
    val date: Date = new Date()
    val mp = MessageProperties(
      ContentType → "content type",
      ContentEncoding → "encoding",
      Type → "type",
      Timestamp → date,
      MessageId → "message id",
      ReplyTo → "reply to",
      Key.DeliveryMode → Persistent,
      UserId → "user id",
      Expiration → "expiration",
      Priority → 2,
      Headers → Map(),
      CorrelationId → "correlation id",
      AppId → "app id"
    )
    val bp = mp.toBasicProperties
    bp.getContentType should be ("content type")
    bp.getContentEncoding should be ("encoding")
    bp.getType should be ("type")
    bp.getTimestamp should be (date)
    bp.getMessageId should be ("message id")
    bp.getReplyTo should be ("reply to")
    bp.getDeliveryMode should be (2)
    bp.getUserId should be ("user id")
    bp.getExpiration should be ("expiration")
    bp.getPriority should be (2)
    bp.getHeaders should be (new java.util.HashMap)
    bp.getCorrelationId should be ("correlation id")
    bp.getAppId should be ("app id")
  }

  it should "be extractable" in {
    val mp = MessageProperties(
      ContentType → "content type",
      ContentEncoding → "encoding")

    val ContentType(contentType) = mp
    contentType should be ("content type")
  }

  it should "extract or null" in {
    val mp = MessageProperties(
      ContentType → "content type",
      ContentEncoding → "encoding")

    mp.getOrNull(ContentEncoding) should be ("encoding")
    mp.getOrNull(Priority) should be (null)
  }

  it should "be appendable" in {
    val mp1 = MessageProperties(
      ContentType → "content type")
    val mp2 = MessageProperties(
      ContentEncoding → "encoding")

    mp1 ++ mp2 should be (MessageProperties(
      ContentType → "content type",
      ContentEncoding → "encoding"))
  }
}
