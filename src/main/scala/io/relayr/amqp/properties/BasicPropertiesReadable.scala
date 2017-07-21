package io.relayr.amqp.properties

import java.util
import java.util.Date

import com.rabbitmq.client.BasicProperties
import io.relayr.amqp.MessageProperties
import io.relayr.amqp.properties.Key._

/**
 * This class is a canary to detect if any properties have been added to the java client's BasicProperties and not into MessageProperties
 */
class BasicPropertiesReadable(mp: MessageProperties) extends BasicProperties {

  override def getContentType: String = mp.getOrNull(ContentType)

  override def getType: String = mp.getOrNull(Type)

  override def getTimestamp: Date = mp.getOrNull(Timestamp)

  override def getMessageId: String = mp.getOrNull(MessageId)

  override def getReplyTo: String = mp.getOrNull(ReplyTo)

  override def getDeliveryMode: Integer = mp.getOrNull(DeliveryMode)

  override def getUserId: String = mp.getOrNull(UserId)

  override def getExpiration: String = mp.getOrNull(Expiration)

  override def getPriority: Integer = mp.getOrNull(Priority)

  override def getHeaders: util.Map[String, AnyRef] = mp.getOrNull(Headers)

  override def getCorrelationId: String = mp.getOrNull(CorrelationId)

  override def getAppId: String = mp.getOrNull(AppId)

  override def getContentEncoding: String = mp.getOrNull(ContentEncoding)
}
