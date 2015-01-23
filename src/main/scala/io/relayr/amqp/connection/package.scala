package io.relayr.amqp

import com.rabbitmq.client.Channel

import scala.concurrent.ExecutionContext

package object connection {
  private[connection] trait ChannelSessionProvider {
    def withChannel[T](expression: (Channel) ⇒ T): T
  }

  type ChannelFactory = (ChannelSessionProvider, ExecutionContext) ⇒ ChannelOwner
}
