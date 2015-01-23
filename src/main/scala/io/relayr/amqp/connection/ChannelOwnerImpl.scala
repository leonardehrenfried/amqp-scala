package io.relayr.amqp.connection

import io.relayr.amqp._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Provides different use cases for a channel
 * @param cs provides the channel to be used for these strategies, after a reconnection of the underlying connection this channel would change
 * @param executionContext for any blocking channel management calls to the underlying java client
 */
private[connection] class ChannelOwnerImpl(val cs: ChannelSessionProvider, executionContext: ExecutionContext) extends ChannelOwner {
  /**
   * Adds a handler to respond to RPCs on a particular binding
   * @param binding specifies the exxchange, queue and routing key to bind the listener of the rpcServer to
   * @param handler function to call with RPC calls
   * @param ec executor for running the handler
   */
  override def rpcServer(binding: Binding)(handler: (Message) ⇒ Future[Message])(implicit ec: ExecutionContext): RPCServer = ???

  /**
   * Creates a client for making RPCs via amqp
   * @param exchange to send rpc calls to
   * @param routingKey for rpc messages
   */
  override def rpcClient(exchange: String, routingKey: String)(deliveryMode: DeliveryMode): RPCClient = ???
}

private[connection] object ChannelOwnerImpl extends ChannelFactory {
  def apply(cs: ChannelSessionProvider, executionContext: ExecutionContext) = new ChannelOwnerImpl(cs, executionContext)
}