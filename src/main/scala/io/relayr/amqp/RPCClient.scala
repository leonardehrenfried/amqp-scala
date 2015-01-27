package io.relayr.amqp

import io.relayr.amqp.concurrent.ScheduledExecutor
import io.relayr.amqp.rpc.client.{ RPCClientImpl, ResponseDispatcher }

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object RPCClient {
  def apply(channel: ChannelOwner): RPCClient =
    new RPCClientImpl(publishChannel = channel, new ResponseDispatcher(listenChannel = channel, new ScheduledExecutor(1)))
}

trait RPCClient {
  def newMethod(routingDescriptor: RoutingDescriptor, timeout: FiniteDuration): RPCMethod
}

trait RPCMethod {
  def apply(message: Message): Future[Message]
}
