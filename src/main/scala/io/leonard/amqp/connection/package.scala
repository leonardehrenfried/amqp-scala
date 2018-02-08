package io.leonard.amqp

import com.rabbitmq.client.Channel

package object connection {
  private[connection] trait ChannelSessionProvider {
    def withChannel[T](expression: (Channel) ⇒ T): T
  }

  type ChannelFactory = (Channel, Event ⇒ Unit) ⇒ ChannelOwner
}
