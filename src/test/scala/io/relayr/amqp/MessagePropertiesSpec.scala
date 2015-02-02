package io.relayr.amqp

import java.util.Date

import io.relayr.amqp.DeliveryMode.Persistent
import io.relayr.amqp.properties.Key
import io.relayr.amqp.properties.Key._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }
import io.relayr.amqp.MessageProperties.ArrowAssoc

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
}
