package io.relayr.amqp.rpc.client

import com.rabbitmq.client.BasicProperties
import io.relayr.amqp._
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
    outboundChannel.send _ expects (routingDescriptor, message, *) onCall { (RoutingDescriptor, Message, ps: BasicProperties) ⇒ assert(ps.getCorrelationId.equals("correlation") && ps.getReplyTo.equals("replyTo")) }

    val future: Future[Message] = method(message)

    future should be (promise.future)
  }
}
