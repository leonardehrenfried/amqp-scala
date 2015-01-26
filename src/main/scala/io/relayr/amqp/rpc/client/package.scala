package io.relayr.amqp.rpc

import io.relayr.amqp._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

package object client {
  object RPCClient {
    def apply(channel: ChannelOwner): RPCClient = ???

  }

  trait RPCClient {
    def apply(routingDescriptor: RoutingDescriptor, timeout: FiniteDuration): RPCMethod
  }

  trait RPCMethod {
    def apply(message: Message): Future[Message]
  }

}
