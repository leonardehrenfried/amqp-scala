package io.relayr.amqp.rpc.client

import io.relayr.amqp._
import io.relayr.amqp.concurrent.ScheduledExecutor
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpecLike }

import scala.concurrent.Await
import scala.concurrent.duration._

class ResponseDispatcherSpec extends WordSpecLike with Matchers with MockFactory {

  "ResponseDispatcher" when {
    "first created and created listening queue" should {
      val channel = mock[ChannelOwner]
      channel.declareQueue _ expects QueueDeclare(None) returning QueueDeclared("queue name")
      var consumer: Delivery ⇒ Unit = null
      channel.addConsumer _ expects (QueuePassive("queue name"), false, *) onCall { (_, _, _consumer) ⇒
        consumer = _consumer
        mock[Closeable]
      }
      val responseDispatcher = new ResponseDispatcher(channel, new ScheduledExecutor(1))

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

      "timeout promises" in {
        val response = responseDispatcher.prepareResponse(1 milli)
        intercept[RPCTimeout](Await.result(response.response, 10 millis))
      }

      "an uncorrelated message is delivered" in {
        // TODO events
      }
    }

    // TODO reconnection
    "the AMQP connection is reconnected" should {}
  }
}
