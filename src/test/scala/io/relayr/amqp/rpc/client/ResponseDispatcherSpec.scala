package io.relayr.amqp.rpc.client

import io.relayr.amqp._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpecLike }

import scala.concurrent.Await
import scala.concurrent.duration._

class ResponseDispatcherSpec extends WordSpecLike with Matchers with MockFactory {

  "ResponseDispatcher" when {
    "first created and created listening queue" should {
      val channel = mock[ChannelOwner]
      channel.createQueue _ expects QueueDeclare(None) returning QueueDeclared("queue name")
      var consumer: Delivery ⇒ Unit = null
      channel.addConsumer _ expects ("queue name", false, *) onCall ((_, _, _consumer) ⇒ consumer = _consumer)
      val responseDispatcher = new ResponseDispatcher(channel)

      "return queue name as response replyTo" in {
        responseDispatcher.prepareResponse(1 second).replyTo should be ("queue name")
      }

      "create unique correlationIds" in {
        val response1 = responseDispatcher.prepareResponse(1 second)
        val response2 = responseDispatcher.prepareResponse(1 second)
        response1.correlationId should not be response2.correlationId
      }

      "return an uncompleted future" in {
        responseDispatcher.prepareResponse(1 second).response.isCompleted should be (false)
      }

      "complete Future when response is received" in {
        val response = responseDispatcher.prepareResponse(1 second)
        val delivery = mock[Delivery]
        val message: Message = Message("", "", ByteArray(Array()))
        delivery.correlationId _ expects () returning response.correlationId
        delivery.message _ expects () returning message
        consumer(delivery)
        Await.result(response.response, 1 second) should be (message)
      }
    }
  }
}
