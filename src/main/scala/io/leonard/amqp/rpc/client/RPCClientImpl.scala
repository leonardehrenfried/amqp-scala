package io.leonard.amqp.rpc.client

import io.leonard.amqp._
import io.leonard.amqp.properties.Key.{ CorrelationId, ReplyTo }

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

private[amqp] class RPCClientImpl(publishChannel: ChannelOwner, responseController: ResponseController) extends RPCClient {
  override def newMethod(routingDescriptor: RoutingDescriptor, timeout: FiniteDuration): RPCMethod =
    new RPCMethodImpl(routingDescriptor, timeout)

  class RPCMethodImpl(routingDescriptor: RoutingDescriptor, timeout: FiniteDuration) extends RPCMethod {

    override def apply(message: Message): Future[Message] = {
      val ResponseSpec(correlationId, replyTo, response, onReturn) = responseController.prepareResponse(timeout)
      publishChannel.send(routingDescriptor, message.withProperties(CorrelationId → correlationId, ReplyTo → replyTo), onReturn, timeout)
      response
    }
  }
}
