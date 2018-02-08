package io.leonard.amqp.rpc.client

import io.leonard.amqp._
import io.leonard.amqp.concurrent.ScheduledExecutor
import io.leonard.amqp.properties.Key.CorrelationId
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpecLike }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ResponseDispatcherSpec extends WordSpecLike with Matchers with MockFactory {

  "ResponseDispatcher" when {
    "first created and created listening queue" should {
      val channel = mock[ChannelOwner]
      channel.declareQueue _ expects QueueDeclare(None) returning "queue name"
      var consumer: Message ⇒ Unit = null
      (channel.addConsumer(_: Queue, _: Message ⇒ Unit)) expects (QueuePassive("queue name"), *) onCall { (_, _consumer) ⇒
        consumer = _consumer
        mock[Closeable]
      }
      val responseDispatcher = new ResponseDispatcher(channel, ScheduledExecutor.defaultScheduledExecutor)

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
        val message = Message.JSONString("json").withProperties(CorrelationId → response.correlationId)
        consumer(message)
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
