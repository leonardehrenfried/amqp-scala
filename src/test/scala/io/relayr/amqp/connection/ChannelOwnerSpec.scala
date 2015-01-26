package io.relayr.amqp.connection

import com.rabbitmq.client.{ AMQP, Channel, Consumer }
import io.relayr.amqp.{ Message, ByteArray }
import io.relayr.amqp.rpc.client.Delivery
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

    "addConsumer" should {
      var javaConsumer: Consumer = null

      "add a consumer" in {
        (channel.basicConsume: (String, Boolean, Consumer) ⇒ String) expects ("queue name", false, *) onCall { (String, Boolean, c: Consumer) ⇒ javaConsumer = c; "" }

        channelOwner.addConsumer("queue name", false, consumer)
      }

      "build Deliveries" in {
        (channel.basicConsume: (String, Boolean, Consumer) ⇒ String) expects ("queue name", false, *) onCall { (String, Boolean, c: Consumer) ⇒ javaConsumer = c; "" }

        channelOwner.addConsumer("queue name", false, consumer)

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

        javaConsumer.handleDelivery("", null, properties, body)
      }
    }
  }
}
