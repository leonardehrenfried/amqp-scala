package io.relayr.amqp.connection

import com.rabbitmq.client._
import io.relayr.amqp.{ ChannelOwner, ConnectionHolder, EventHooks, ReconnectionStrategy }

import scala.concurrent.{ ExecutionContext, blocking }

private[connection] class ReconnectingConnectionHolder(factory: ConnectionFactory, reconnectionStrategy: Option[ReconnectionStrategy], eventHooks: EventHooks, implicit val executionContext: ExecutionContext, channelFactory: ChannelFactory) extends ConnectionHolder {

  private var currentConnection: CurrentConnection = new CurrentConnection(None, Map())

  reconnect()

  private def reconnect(): Unit = this.synchronized {
    blocking {
      val conn = factory.newConnection()
      conn.addShutdownListener(new ShutdownListener {
        override def shutdownCompleted(cause: ShutdownSignalException): Unit = {
          reconnectionStrategy.foreach(r ⇒ r.scheduleReconnection { reconnect() })
        }
      })
      def recreateChannels(channelKeys: Iterable[ChannelKey]): Map[ChannelKey, Channel] = {
        channelKeys.map(channelKey ⇒
          (channelKey, createChannel(conn, channelKey.qos))
        ).toMap
      }
      currentConnection = new CurrentConnection(Some(conn), recreateChannels(currentConnection.channelMappings.keys))
    }
  }

  private def createChannel(conn: Connection, qos: Int): Channel = blocking {
    val channel = conn.createChannel()
    // NOTE there may be other parameters possible to set up on the connection at the start
    channel.basicQos(qos)
    channel
  }

  /**
   * Create a new channel multiplexed over this connection.
   * @note Blocks on creation of the underlying channel
   */
  override def newChannel(qos: Int): ChannelOwner = this.synchronized {
    ensuringConnection { c ⇒
      val key: ChannelKey = new ChannelKey(qos)
      currentConnection.channelMappings = currentConnection.channelMappings + (key -> createChannel(c, qos))
      channelFactory(new InternalChannelSessionProvider(key), executionContext)
    }
  }

  /** Close the connection. */
  override def close(): Unit = this.synchronized {
    currentConnection.connection.foreach(c ⇒ blocking { c.close() })
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
