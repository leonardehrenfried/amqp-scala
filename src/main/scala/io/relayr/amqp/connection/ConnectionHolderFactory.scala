package io.relayr.amqp.connection

import java.util.concurrent.{ ThreadFactory, ExecutorService }
import javax.net.SocketFactory

import com.rabbitmq.client.{ ExceptionHandler, SocketConfigurator, SaslConfig, ConnectionFactory }
import io.relayr.amqp.ReconnectionStrategy.{ JavaClientFixedReconnectDelay, NoReconnect }
import io.relayr.amqp.{ ConnectionHolder, EventHooks, ReconnectionStrategy }

import scala.collection.JavaConversions

private[amqp] abstract class ConnectionHolderFactory {
  def _uri: String
  def _requestedChannelMax: Option[Int]
  def _requestedFrameMax: Option[Int]
  def _requestedHeartbeat: Option[Int]
  def _connectionTimeout: Option[Int]
  def _shutdownTimeout: Option[Int]
  def _clientProperties: Map[String, AnyRef]
  def _socketFactory: Option[SocketFactory]
  def _saslConfig: Option[SaslConfig]
  def _sharedExecutor: Option[ExecutorService]
  def _threadFactory: Option[ThreadFactory]
  def _socketConfigurator: Option[SocketConfigurator]
  def _exceptionHandler: Option[ExceptionHandler]
  def _automaticRecovery: Option[Boolean]
  def _topologyRecovery: Option[Boolean]
  def _networkRecoveryInterval: Option[Long]
  def _reconnectionStrategy: ReconnectionStrategy
  def _eventHooks: EventHooks

  def build(): ConnectionHolder = {
    val cf = new ConnectionFactory
    cf.setUri(_uri)
    _requestedChannelMax.foreach(cf.setRequestedChannelMax)
    _requestedFrameMax.foreach(cf.setRequestedFrameMax)
    _requestedHeartbeat.foreach(cf.setRequestedHeartbeat)
    _connectionTimeout.foreach(cf.setConnectionTimeout)
    _shutdownTimeout.foreach(cf.setShutdownTimeout)
    if (_clientProperties.nonEmpty) cf.setClientProperties(JavaConversions.mapAsJavaMap(_clientProperties))
    _socketFactory.foreach(cf.setSocketFactory)
    _saslConfig.foreach(cf.setSaslConfig)
    _sharedExecutor.foreach(cf.setSharedExecutor)
    _threadFactory.foreach(cf.setThreadFactory)
    _socketConfigurator.foreach(cf.setSocketConfigurator)
    _exceptionHandler.foreach(cf.setExceptionHandler)
    _automaticRecovery.foreach(cf.setAutomaticRecoveryEnabled)
    _topologyRecovery.foreach(cf.setTopologyRecoveryEnabled)
    _networkRecoveryInterval.foreach(cf.setNetworkRecoveryInterval)

    _reconnectionStrategy match {
      case NoReconnect ⇒ ()
      case JavaClientFixedReconnectDelay(networkRecoveryInterval) ⇒
        cf.setAutomaticRecoveryEnabled(true)
        cf.setNetworkRecoveryInterval(networkRecoveryInterval.toMillis)
      case _ ⇒ throw new IllegalArgumentException(s"Unsupported strategy ${_reconnectionStrategy}")
    }
    new ReconnectingConnectionHolder(cf, _eventHooks.event, ChannelOwnerImpl)
  }
}
