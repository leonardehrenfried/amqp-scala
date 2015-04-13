package io.relayr.amqp

import java.util.concurrent.{ ExecutorService, ThreadFactory }

import com.rabbitmq.client.{ Connection, ExceptionHandler, SocketConfigurator }
import io.relayr.amqp.connection.{ ChannelWrapper, ConnectionHolderFactory, ConnectionWrapper }

/** Holds a connection, the underlying connection may be replaced if it fails */
trait ConnectionHolder {
  /** Create a new channel multiplexed over this connection */
  def newChannel(qos: Int): ChannelOwner

  /** Create a new channel multiplexed over this connection with a qos level set */
  def newChannel(): ChannelOwner

  def close()
}

trait ConnectionHolderBuilder {
  def build(): ConnectionHolder

  def requestedChannelMax(i: Int): ConnectionHolderBuilder
  def requestedFrameMax(i: Int): ConnectionHolderBuilder
  def requestedHeartbeat(i: Int): ConnectionHolderBuilder
  def connectionTimeout(i: Int): ConnectionHolderBuilder
  def shutdownTimeout(i: Int): ConnectionHolderBuilder
  def clientProperties(m: Map[String, Object]): ConnectionHolderBuilder
  def sharedExecutor(s: ExecutorService): ConnectionHolderBuilder
  def threadFactory(s: ThreadFactory): ConnectionHolderBuilder
  def socketConfigurator(i: SocketConfigurator): ConnectionHolderBuilder
  def exceptionHandler(i: ExceptionHandler): ConnectionHolderBuilder
  def topologyRecovery(i: Boolean): ConnectionHolderBuilder
  def reconnectionStrategy(i: ReconnectionStrategy): ConnectionHolderBuilder
  def eventHooks(i: EventHooks): ConnectionHolderBuilder
}

/**
 * Create a connection using the builder:
 * {{{
 * val connection = ConnectionHolder.builder("amqps://guest:password@host:port")
 *   .eventHooks(EventHooks(eventListener))
 *   .reconnectionStrategy(ReconnectionStrategy.JavaClientFixedReconnectDelay(1 second))
 *   .build()
 * }}}
 */
object ConnectionHolder {
  def builder(uri: String): ConnectionHolderBuilder =
    new Builder(uri)

  private[amqp] case class Builder(
      _uri: String,
      _requestedChannelMax: Option[Int] = None,
      _requestedFrameMax: Option[Int] = None,
      _requestedHeartbeat: Option[Int] = None,
      _connectionTimeout: Option[Int] = None,
      _shutdownTimeout: Option[Int] = None,
      _clientProperties: Map[String, AnyRef] = Map.empty,
      //      _socketFactory: Option[SocketFactory] = None,
      //      _saslConfig: Option[SaslConfig] = None,
      _sharedExecutor: Option[ExecutorService] = None,
      _threadFactory: Option[ThreadFactory] = None,
      _socketConfigurator: Option[SocketConfigurator] = None,
      _exceptionHandler: Option[ExceptionHandler] = None,
      //      _automaticRecovery: Option[Boolean] = None,
      _topologyRecovery: Option[Boolean] = None,
      //      _networkRecoveryInterval: Option[Long] = None,
      _reconnectionStrategy: ReconnectionStrategy = ReconnectionStrategy.default,
      _eventHooks: EventHooks = EventHooks(PartialFunction.empty)) extends ConnectionHolderFactory with ConnectionHolderBuilder {

    def requestedChannelMax(i: Int) = copy(_requestedChannelMax = Some(i))
    def requestedFrameMax(i: Int) = copy(_requestedFrameMax = Some(i))
    def requestedHeartbeat(i: Int) = copy(_requestedHeartbeat = Some(i))
    def connectionTimeout(i: Int) = copy(_connectionTimeout = Some(i))
    def shutdownTimeout(i: Int) = copy(_shutdownTimeout = Some(i))
    def clientProperties(m: Map[String, Object]) = copy(_clientProperties = m)
    //    def socketFactory(s: SocketFactory) = copy(_socketFactory = Some(s))
    //    def saslConfig(s: SaslConfig) = copy(_saslConfig = Some(s))
    def sharedExecutor(s: ExecutorService) = copy(_sharedExecutor = Some(s))
    def threadFactory(s: ThreadFactory) = copy(_threadFactory = Some(s))
    def socketConfigurator(i: SocketConfigurator) = copy(_socketConfigurator = Some(i))
    def exceptionHandler(i: ExceptionHandler) = copy(_exceptionHandler = Some(i))
    //    def automaticRecovery(i: Boolean) = copy(_automaticRecovery = Some(i))
    def topologyRecovery(i: Boolean) = copy(_topologyRecovery = Some(i))
    //    def networkRecoveryInterval(i: Long) = copy(_networkRecoveryInterval = Some(i))
    def reconnectionStrategy(i: ReconnectionStrategy) = copy(_reconnectionStrategy = i)
    def eventHooks(i: EventHooks) = copy(_eventHooks = i)

    protected def createConnectionHolder(conn: Connection): ConnectionWrapper = {
      new ConnectionWrapper(conn, _eventHooks.event, ChannelWrapper)
    }
  }
}
