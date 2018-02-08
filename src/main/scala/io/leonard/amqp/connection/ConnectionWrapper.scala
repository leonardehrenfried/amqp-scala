package io.leonard.amqp.connection

import com.rabbitmq.client._
import io.leonard.amqp.Event.{ ChannelEvent, ConnectionEvent }
import io.leonard.amqp._
import io.leonard.amqp.connection.Listeners._

import scala.concurrent.blocking
import scala.language.postfixOps

private[amqp] class ConnectionWrapper(conn: Connection, eventConsumer: Event ⇒ Unit, channelFactory: ChannelFactory) extends ConnectionHolder {

  conn.addShutdownListener(shutdownListener(cause ⇒
    eventConsumer(ConnectionEvent.ConnectionShutdown)
  ))

  private def createChannel(conn: Connection, qos: Option[Int]): Channel = blocking {
    val channel = conn.createChannel()
    // NOTE there may be other parameters possible to set up on the connection at the start
    qos.foreach(channel.basicQos)
    eventConsumer(ChannelEvent.ChannelOpened(channel.getChannelNumber, qos))
    channel.addReturnListener(new ReturnListener {
      override def handleReturn(replyCode: Int, replyText: String, exchange: String, routingKey: String, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
        eventConsumer(ChannelEvent.MessageReturned(replyCode, replyText, exchange, routingKey, Message.Raw(body, properties)))
    })
    channel.addShutdownListener(shutdownListener(cause ⇒
      eventConsumer(ChannelEvent.ChannelShutdown)
    ))
    channel
  }

  /**
   * Create a new channel multiplexed over this connection.
   * @note Blocks on creation of the underlying channel
   */
  override def newChannel(qos: Int): ChannelOwner = newChannel(Some(qos))

  override def newChannel(): ChannelOwner = newChannel(None)

  /**
   * Create a new channel multiplexed over this connection.
   * @note Blocks on creation of the underlying channel
   */
  def newChannel(qos: Option[Int] = None): ChannelOwner = this.synchronized {
    channelFactory(createChannel(conn, qos), eventConsumer)
  }

  /** Close the connection. */
  override def close(): Unit = this.synchronized {
    blocking { conn.close() }
  }
}
