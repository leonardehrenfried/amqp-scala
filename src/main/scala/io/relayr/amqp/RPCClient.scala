package io.relayr.amqp

import io.relayr.amqp.concurrent.ScheduledExecutor
import io.relayr.amqp.rpc.client.{ RPCTimeout, RPCClientImpl, ResponseDispatcher }

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object RPCClient {
  def apply(channel: ChannelOwner): RPCClient =
    new RPCClientImpl(publishChannel = channel, new ResponseDispatcher(listenChannel = channel, new ScheduledExecutor(1)))
}

trait RPCClient {
  def newMethod(routingDescriptor: RoutingDescriptor, timeout: FiniteDuration): RPCMethod
}

/**
 * RPC method over AMQP.
 *
 * The future either completes with the correlating response message, an RPCTimeout or UndeliveredException
 */
@throws[RPCTimeout]("if no reply comes before the method timeout")
@throws[UndeliveredException]("if the request is returned undelivered by the broker")
trait RPCMethod extends (Message ⇒ Future[Message])
