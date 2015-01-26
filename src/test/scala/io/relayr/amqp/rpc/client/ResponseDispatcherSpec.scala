package io.relayr.amqp.rpc.client

import io.relayr.amqp._
import org.scalatest.{ Matchers, WordSpecLike }
import org.scalamock.scalatest.MockFactory

import scala.concurrent.duration._

class ResponseDispatcherSpec extends WordSpecLike with Matchers with MockFactory {
  "ResponseDispatcher" when {
    "first created and created listening queue" should {
      val channel = mock[ChannelOwner]
      channel.createQueue _ expects QueueDeclare(None) returning QueueDeclared("queue name")
      val responseDispatcher = new ResponseDispatcher(channel)

      "return queue name as response replyTo" in {
        responseDispatcher.prepareResponse(1 second).replyTo should be ("queue name")
      }

      "correlationIds are unique" in {
        val response1: ResponseSpec = responseDispatcher.prepareResponse(1 second)
        val response2: ResponseSpec = responseDispatcher.prepareResponse(1 second)
        response1.correlationId should not be response2.correlationId
      }
    }
  }
}
