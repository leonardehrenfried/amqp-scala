package io.relayr.amqp.rpc.client

import com.rabbitmq.client._
import io.relayr.amqp.{ Message, ChannelOwner, RoutingDescriptor }

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

private[client] class RPCClientImpl(publishChannel: ChannelOwner, responseController: ResponseController) extends RPCClient {
  override def newMethod(routingDescriptor: RoutingDescriptor, timeout: FiniteDuration): RPCMethod =
    new RPCMethodImpl(routingDescriptor, timeout)

  class RPCMethodImpl(routingDescriptor: RoutingDescriptor, timeout: FiniteDuration) extends RPCMethod {

    override def apply(message: Message): Future[Message] = {
      val ResponseSpec(correlationId, replyTo, response) = responseController.prepareResponse(timeout)
      val basicProperties: BasicProperties = requestProperties(correlationId, replyTo)
      publishChannel.send(routingDescriptor, message, basicProperties)
      response
    }

    def requestProperties(correlationId: String, replyTo: String): BasicProperties = {
      new AMQP.BasicProperties.Builder().correlationId(correlationId).replyTo(replyTo).build()
    }
  }
}
