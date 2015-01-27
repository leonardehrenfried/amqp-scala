package io.relayr.amqp.connection

import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk
import com.rabbitmq.client.{ AMQP, Channel, Consumer }
import io.relayr.amqp._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpecLike }

import scala.concurrent.ExecutionContext

class ChannelOwnerSpec extends WordSpecLike with Matchers with MockFactory {

  "ChannelOwner" when {
    val channel = mock[Channel]
    val cs = new ChannelSessionProvider {
      override def withChannel[T](expression: (Channel) ⇒ T): T = expression(channel)
    }
    var channelOwner = new ChannelOwnerImpl(cs, ExecutionContext.global)
    val consumer = mockFunction[Delivery, Unit]

    val QUEUE_NAME: String = "queue name"

    "addConsumer" should {
      var javaConsumer: Consumer = null

      "check that the queue exists and then add a consumer" in {
        channel.queueDeclarePassive _ expects QUEUE_NAME returning new DeclareOk(QUEUE_NAME, 1, 1)
        (channel.basicConsume: (String, Boolean, Consumer) ⇒ String) expects (QUEUE_NAME, false, *) onCall { (String, Boolean, c: Consumer) ⇒ javaConsumer = c; "" }

        channelOwner.addConsumer(QueuePassive(QUEUE_NAME), false, consumer)
      }

      "send" should {
        "send a message" in {
          // TODO how does scalamock work?
          //          val message = Message("type", "encoding", ByteArray(Array(1: Byte)))
          //          val basicProperties = new AMQP.BasicProperties.Builder().appId("app").build()

          //          (channel.basicPublish(_: String, _: String, _: AMQP.BasicProperties, _: Array[Byte])) expects (Exchange.Direct.name, QUEUE_NAME, *, Array(1: Byte))
          //          channelOwner.send(Exchange.Direct.route(QUEUE_NAME, DeliveryMode.NotPersistent), message, basicProperties)
        }
      }

      "build Deliveries" in {
        channel.queueDeclarePassive _ expects QUEUE_NAME returning new DeclareOk(QUEUE_NAME, 1, 1)
        (channel.basicConsume: (String, Boolean, Consumer) ⇒ String) expects (QUEUE_NAME, false, *) onCall { (String, Boolean, c: Consumer) ⇒ javaConsumer = c; "" }

        channelOwner.addConsumer(QueuePassive(QUEUE_NAME), false, consumer)

        val body: Array[Byte] = Array(1: Byte)
        consumer expects where {
          (delivery: Delivery) ⇒
            delivery.message.equals(Message("type", "encoding", ByteArray(body))) &&
              delivery.correlationId.equals("correlation")
        }

        val properties: AMQP.BasicProperties = mock[AMQP.BasicProperties]
        properties.getContentType _ expects () returning "type"
        properties.getContentEncoding _ expects () returning "encoding"
        properties.getCorrelationId _ expects () returning "correlation"
        properties.getReplyTo _ expects () returning "replyChannel"

        javaConsumer.handleDelivery("", null, properties, body)
      }
    }

    "createQueue" should {
      "create a new queue" in {
        //        (channel.queueDeclare _: (String, Boolean, Boolean, Boolean, java.util.Map[String, Object]) ⇒ Queue.DeclareOk) expects (QUEUE_NAME, false, false, false, null: java.util.Map[String, Object])
        //        TODO : I cant make scalamock work with java maps
      }

    }
  }
}
