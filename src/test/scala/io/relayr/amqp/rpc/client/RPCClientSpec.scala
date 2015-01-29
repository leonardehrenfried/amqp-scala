package io.relayr.amqp.rpc.client

import io.relayr.amqp._
import io.relayr.amqp.properties.Key.{ CorrelationId, ReplyTo }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }

class RPCClientSpec extends FlatSpec with Matchers with MockFactory {

  "RPCClient" should "create methods, which send messages and use the ResponseDispatcher" in {
    val outboundChannel = mock[ChannelOwner]
    val responseController = mock[ResponseController]
    val client = new RPCClientImpl(outboundChannel, responseController)

    val routingDescriptor: RoutingDescriptor = Exchange.Direct.route("queue name", DeliveryMode.NotPersistent)
    val method: RPCMethod = client.newMethod(routingDescriptor, 500 millis)

    val promise = Promise[Message]()
    responseController.prepareResponse _ expects (500 millis) returning ResponseSpec("correlation", "replyTo", promise.future)

    val message = Message.JSONString("json")
    outboundChannel.send _ expects (routingDescriptor, *) onCall { (RoutingDescriptor, m: Message) ⇒
      val Message.JSONString(string) = m
      assert(string == "json")
      assert(m.property(CorrelationId).equals(Some("correlation")) && m.property(ReplyTo).equals(Some("replyTo")))
    }

    val future: Future[Message] = method(message)

    future should be (promise.future)
  }
}
