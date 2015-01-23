package io.relayr.amqp.connection

import com.rabbitmq.client.Channel
import io.relayr.amqp.ChannelOwner

import scala.concurrent.ExecutionContext

private[connection] trait ChannelSessionProvider {
  def withChannel[T](expression: (Channel) ⇒ T): T
}

private[connection] trait ChannelFactory {
  def apply(cs: ChannelSessionProvider, executionContext: ExecutionContext): ChannelOwner
}

