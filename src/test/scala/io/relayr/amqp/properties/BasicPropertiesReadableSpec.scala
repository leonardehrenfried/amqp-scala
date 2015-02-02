package io.relayr.amqp.properties

import java.util
import java.util.Date

import com.rabbitmq.client.AMQP.BasicProperties.Builder
import io.relayr.amqp.MessageProperties
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpec }

import scala.collection.JavaConversions

class BasicPropertiesReadableSpec extends WordSpec with Matchers with MockFactory {

  "BasicPropertiesReadable" should {
    val CONTENT_TYPE: String = "content type"
    val TYPE = "type"
    val TIMESTAMP = new Date
    val MESSAGE_ID = "message id"
    val REPLY_TO = "reply to"
    val DELIVERY_MODE = 1
    val USER_ID = "user id"
    val EXPIRATION = "expiration"
    val PRIORITY = 2
    val HEADERS: util.Map[String, AnyRef] = JavaConversions.mapAsJavaMap(Map("key" → "value"))
    val CORRELATION_ID = "correlation id"
    val APP_ID = "app id"
    val CONTENT_ENCODING = "content encoding"

    val bpr = new BasicPropertiesReadable(MessageProperties(new Builder()
      .contentType(CONTENT_TYPE)
      .`type`(TYPE)
      .timestamp(TIMESTAMP)
      .messageId(MESSAGE_ID)
      .replyTo(REPLY_TO)
      .deliveryMode(DELIVERY_MODE)
      .userId(USER_ID)
      .expiration(EXPIRATION)
      .priority(PRIORITY)
      .headers(HEADERS)
      .correlationId(CORRELATION_ID)
      .appId(APP_ID)
      .contentEncoding(CONTENT_ENCODING)
      .build()))

    s"read contentType as it is written" in {
      bpr.getContentType should be (CONTENT_TYPE)
    }

    s"read type as it is written" in {
      bpr.getType should be (TYPE)
    }

    s"read timestamp as it is written" in {
      bpr.getTimestamp should be (TIMESTAMP)
    }

    s"read messageid as it is written" in {
      bpr.getMessageId should be (MESSAGE_ID)
    }

    s"read replyTo as it is written" in {
      bpr.getReplyTo should be (REPLY_TO)
    }

    s"read deliveryMode as it is written" in {
      bpr.getDeliveryMode should be (DELIVERY_MODE)
    }

    s"read UserId as it is written" in {
      bpr.getUserId should be (USER_ID)
    }

    s"read expiration as it is written" in {
      bpr.getExpiration should be (EXPIRATION)
    }

    s"read priority as it is written" in {
      bpr.getPriority should be (PRIORITY)
    }

    s"read headers as it is written" in {
      bpr.getHeaders should be (HEADERS)
    }

    s"read correlationId as it is written" in {
      bpr.getCorrelationId should be (CORRELATION_ID)
    }

    s"read appId as it is written" in {
      bpr.getAppId should be (APP_ID)
    }

    s"read contentEncoding as it is written" in {
      bpr.getContentEncoding should be (CONTENT_ENCODING)
    }
  }
}
