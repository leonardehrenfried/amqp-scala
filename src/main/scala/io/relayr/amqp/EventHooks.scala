package io.relayr.amqp

import java.net.InetAddress

import scala.concurrent.duration.FiniteDuration

object EventHooks {
  def apply(f: Event ⇒ Unit): EventHooks = new EventHooks {
    override def event(event: Event): Unit = f(event)
  }

  def apply(pf: PartialFunction[Event, Unit]): EventHooks = new EventHooks() {
    override def event(e: Event) =
      if (pf.isDefinedAt(e))
        pf(e)
  }

  def apply(): EventHooks = new EventHooks {
    override def event(event: Event): Unit = ()
  }
}

trait EventHooks {
  def event(event: Event): Unit
}

sealed trait Event

object Event {

  sealed trait ConnectionEvent extends Event

  object ConnectionEvent {

    case class ConnectionEstablished(address: InetAddress, port: Int, heartbeatInterval: FiniteDuration) extends ConnectionEvent

    case object ConnectionShutdown extends ConnectionEvent
  }

  sealed trait ChannelEvent extends Event

  object ChannelEvent {

    /** Delivered when a channel is opened */
    case class ChannelOpened(channelNumber: Int, qos: Option[Int]) extends ChannelEvent

    /** Delivered when a channel is closed, there is information about what was closed, how and why available but we haven't investigated extracting it */
    case object ChannelShutdown extends ChannelEvent

    /* Delivered when a message is returned as undelivered */
    case class MessageReturned(replyCode: Int, replyText: String, exchange: String, routingKey: String, message: Message) extends ChannelEvent
  }

  sealed trait RPCServerEvent extends Event

  /** Delivered when the RPC server handler throws an exception or the handler future fails */
  case class HandlerError(e: Throwable) extends RPCServerEvent
}
