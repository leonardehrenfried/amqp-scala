package io.relayr.amqp.connection

import com.rabbitmq.client.AMQP.BasicProperties.Builder
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk
import com.rabbitmq.client.{ Envelope, AMQP, Channel, Consumer }
import io.relayr.amqp._
import io.relayr.amqp.concurrent.ScheduledExecutor
import io.relayr.amqp.properties.Key.{ CorrelationId, ContentEncoding, ContentType }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpecLike }

class ChannelOwnerSpec extends WordSpecLike with Matchers with MockFactory {

  "ChannelOwner" when {
    val envelope: Envelope = new Envelope(2001L, false, "exchange", "routing key")
    val channel = mock[Channel]
    val cs = new ChannelSessionProvider {
      override def withChannel[T](expression: (Channel) ⇒ T): T = expression(channel)
    }
    val eventConsumer: Event ⇒ Unit = mockFunction[Event, Unit]
    channel.addReturnListener _ expects *
    val channelOwner: ChannelOwner = new ChannelOwnerImpl(cs, eventConsumer, ScheduledExecutor.defaultScheduledExecutor)
    val consumer = mockFunction[Message, Unit]

    val QUEUE_NAME: String = "queue name"

    "addConsumer" should {
      var javaConsumer: Consumer = null

      "check that the queue exists and then add a consumer" in {
        channel.queueDeclarePassive _ expects QUEUE_NAME returning new DeclareOk(QUEUE_NAME, 1, 1)
        (channel.basicConsume: (String, Boolean, Consumer) ⇒ String) expects (QUEUE_NAME, true, *) onCall { (String, Boolean, c: Consumer) ⇒ javaConsumer = c; "" }

        channelOwner.addConsumer(QueuePassive(QUEUE_NAME), consumer)
      }

      "send" should {
        "send a message" in {
          //          // TODO how does scalamock work?
          //          val message = Message.Array(Array(1: Byte))
          //          val basicProperties = new AMQP.BasicProperties.Builder().appId("app").build()
          //
          //          (channel.basicPublish: (String, String, AMQP.BasicProperties, Array[Byte]) ⇒ Unit) expects (Exchange.Direct.name, QUEUE_NAME, *, Array(1: Byte))
          //          channelOwner.send(Exchange.Direct.route(QUEUE_NAME, DeliveryMode.NotPersistent), message)
        }
      }

      "build Deliveries with autoAck" in {
        channel.queueDeclarePassive _ expects QUEUE_NAME returning new DeclareOk(QUEUE_NAME, 1, 1)
        (channel.basicConsume: (String, Boolean, Consumer) ⇒ String) expects (QUEUE_NAME, true, *) onCall { (String, Boolean, c: Consumer) ⇒ javaConsumer = c; "" }

        channelOwner.addConsumer(QueuePassive(QUEUE_NAME), consumer)

        val body: Array[Byte] = Array(1: Byte)
        consumer expects where {
          (delivery: Message) ⇒
            delivery.body.equals(ByteArray(body)) &&
              delivery.property(ContentType).equals(Some("type")) &&
              delivery.property(ContentEncoding).equals(Some("encoding")) &&
              delivery.property(CorrelationId).equals(Some("correlation"))
        }

        val properties: AMQP.BasicProperties = new Builder()
          .contentType("type")
          .contentEncoding("encoding")
          .correlationId("correlation")
          .replyTo("replyChannel")
          .build()

        javaConsumer.handleDelivery("", null, properties, body)
      }

      "build Deliveries with manualAck" in {
        val manualConsumer = mockFunction[Message, ManualAcker, Unit]

        channel.queueDeclarePassive _ expects QUEUE_NAME returning new DeclareOk(QUEUE_NAME, 1, 1)
        (channel.basicConsume: (String, Boolean, Consumer) ⇒ String) expects (QUEUE_NAME, false, *) onCall { (String, Boolean, c: Consumer) ⇒ javaConsumer = c; "" }

        channelOwner.addConsumerAckManual(QueuePassive(QUEUE_NAME), manualConsumer)

        val body: Array[Byte] = Array(1: Byte)
        manualConsumer expects where {
          (delivery: Message, acker: ManualAcker) ⇒
            acker.ack() // sends ack & nack, just to test both
            acker.reject(requeue = false)
            delivery.body.equals(ByteArray(body)) &&
              delivery.property(ContentType).equals(Some("type")) &&
              delivery.property(ContentEncoding).equals(Some("encoding")) &&
              delivery.property(CorrelationId).equals(Some("correlation"))
        }

        channel.basicAck _ expects (2001L, false)
        channel.basicReject _ expects (2001L, false)

        val properties: AMQP.BasicProperties = new Builder()
          .contentType("type")
          .contentEncoding("encoding")
          .correlationId("correlation")
          .replyTo("replyChannel")
          .build()

        javaConsumer.handleDelivery("", envelope, properties, body)
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
