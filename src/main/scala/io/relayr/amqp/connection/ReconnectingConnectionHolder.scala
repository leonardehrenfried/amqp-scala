package io.relayr.amqp.connection

import com.rabbitmq.client._
import io.relayr.amqp.{ ChannelOwner, ConnectionHolder, EventHooks, ReconnectionStrategy }

private[connection] class ReconnectingConnectionHolder(factory: ConnectionFactory, reconnectionStrategy: Option[ReconnectionStrategy], eventHooks: EventHooks) extends ConnectionHolder {

  private var currentConnection: CurrentConnection = new CurrentConnection(None, Map())

  currentConnection = connect()

  private def connect(): CurrentConnection = this.synchronized {
    val conn = factory.newConnection()
    conn.addShutdownListener(new ShutdownListener {
      override def shutdownCompleted(cause: ShutdownSignalException): Unit = {
        reconnectionStrategy.foreach(r ⇒ r.scheduleReconnection { currentConnection = connect() })
      }
    })
    new CurrentConnection(Some(conn), currentConnection.channelMappings.map {
      case (channelKey, _) ⇒
        val qos: Int = channelKey.qos
        val channel: Channel = createChannel(conn, qos)
        (channelKey, channel)
    })
  }

  def createChannel(conn: Connection, qos: Int): Channel = {
    val channel = conn.createChannel()
    channel.basicQos(qos)
    channel
  }

  /** Create a new channel multiplexed over this connection */
  override def newChannel(qos: Int): ChannelOwner = this.synchronized {
    ensuringConnection { c ⇒
      val key: ChannelKey = new ChannelKey(qos)
      currentConnection.channelMappings = currentConnection.channelMappings + (key -> createChannel(c, qos))
      new ChannelOwnerImpl(new InternalChannelSessionProvider(key))
    }
  }

  override def close(): Unit = this.synchronized {
    currentConnection.connection.foreach(_.close())
  }

  class InternalChannelSessionProvider(key: ChannelKey) extends ChannelSessionProvider {
    override def withChannel[T](f: (Channel) ⇒ T): T =
      f(currentConnection.channelMappings(key))
  }

  def ensuringConnection[T](function: (Connection) ⇒ T): T = currentConnection.connection match {
    case Some(con) ⇒ function(con)
    // TODO explain and discuss this state exception
    case None      ⇒ throw new IllegalStateException("Not connected")
  }
}

private class CurrentConnection(var connection: Option[Connection], var channelMappings: Map[ChannelKey, Channel])

private class ChannelKey(val qos: Int)

private[connection] trait ChannelSessionProvider {
  def withChannel[T](expression: (Channel) ⇒ T): T
}
