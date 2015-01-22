package io.relayr.amqp.connection

import io.relayr.amqp._

import scala.concurrent.{ ExecutionContext, Future }

private[connection] class ChannelOwnerImpl(val cs: ChannelSessionProvider) extends ChannelOwner {
  /** Adds a handler to respond to RPCs on a particular binding */
  override def rpcServer(binding: Binding)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): RPCServer = ???

  /** Creates a client for making RPCs via amqp */
  override def rpcClient(exchange: String, routingKey: String)(deliveryMode: DeliveryMode): RPCClient = ???
}
