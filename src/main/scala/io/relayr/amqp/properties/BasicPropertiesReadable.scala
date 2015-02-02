package io.relayr.amqp.properties

import java.util
import java.util.Date

import com.rabbitmq.client.BasicProperties
import io.relayr.amqp.MessageProperties
import io.relayr.amqp.properties.Key._

/**
 * This class is a canary to detect if any properties have been added to the java client's BasicProperties and not into MessageProperties
 */
class BasicPropertiesReadable(mp: MessageProperties) extends BasicProperties with UndefinedSetters {

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

/**
 * These are the unimplementaions of all the deprecated setters on the BasicProperties interface, we just have this trait to allow us to implement the methods above
 */
trait UndefinedSetters extends BasicProperties {
  // $COVERAGE-OFF$
  override def setMessageId(messageId: String): Unit = NOT_DEFINED()

  override def setReplyTo(replyTo: String): Unit = NOT_DEFINED()

  override def setDeliveryMode(deliveryMode: Integer): Unit = NOT_DEFINED()

  override def setContentEncoding(contentEncoding: String): Unit = NOT_DEFINED()

  override def setTimestamp(timestamp: Date): Unit = NOT_DEFINED()

  override def setContentType(contentType: String): Unit = NOT_DEFINED()

  override def setPriority(priority: Integer): Unit = NOT_DEFINED()

  override def setType(`type`: String): Unit = NOT_DEFINED()

  override def setExpiration(expiration: String): Unit = NOT_DEFINED()

  override def setHeaders(headers: util.Map[String, AnyRef]): Unit = NOT_DEFINED()

  override def setCorrelationId(correlationId: String): Unit = NOT_DEFINED()

  override def setUserId(userId: String): Unit = NOT_DEFINED()

  override def setAppId(appId: String): Unit = NOT_DEFINED()

  private def NOT_DEFINED() {
    throw new NotImplementedError("Attempted set on immutable")
  }
  // $COVERAGE-ON$
}
