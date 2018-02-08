package io.leonard.amqp.connection

import java.util.concurrent.{ ExecutorService, ThreadFactory }

import com.rabbitmq.client.{ Connection, ConnectionFactory, ExceptionHandler, SocketConfigurator }
import io.leonard.amqp.ReconnectionStrategy.{ JavaClientFixedReconnectDelay, NoReconnect }
import io.leonard.amqp.{ ConnectionHolder, EventHooks, ReconnectionStrategy }

import scala.collection.JavaConverters._

private[amqp] abstract class ConnectionHolderFactory {
  def _uri: String
  def _requestedChannelMax: Option[Int]
  def _requestedFrameMax: Option[Int]
  def _requestedHeartbeat: Option[Int]
  def _connectionTimeout: Option[Int]
  def _shutdownTimeout: Option[Int]
  def _clientProperties: Map[String, AnyRef]
  //  def _socketFactory: Option[SocketFactory]
  //  def _saslConfig: Option[SaslConfig]
  def _sharedExecutor: Option[ExecutorService]
  def _threadFactory: Option[ThreadFactory]
  def _socketConfigurator: Option[SocketConfigurator]
  def _exceptionHandler: Option[ExceptionHandler]
  def _topologyRecovery: Option[Boolean]
  //  def _networkRecoveryInterval: Option[Long]
  def _reconnectionStrategy: ReconnectionStrategy
  def _eventHooks: EventHooks

  private[connection] def buildConnectionFactory: ConnectionFactory = {
    val cf = new ConnectionFactory
    cf.setUri(_uri)
    _requestedChannelMax.foreach(cf.setRequestedChannelMax)
    _requestedFrameMax.foreach(cf.setRequestedFrameMax)
    _requestedHeartbeat.foreach(cf.setRequestedHeartbeat)
    _connectionTimeout.foreach(cf.setConnectionTimeout)
    _shutdownTimeout.foreach(cf.setShutdownTimeout)
    if (_clientProperties.nonEmpty) cf.setClientProperties(_clientProperties.asJava)
    //    _socketFactory.foreach(cf.setSocketFactory)
    //    _saslConfig.foreach(cf.setSaslConfig)
    _sharedExecutor.foreach(cf.setSharedExecutor)
    _threadFactory.foreach(cf.setThreadFactory)
    _socketConfigurator.foreach(cf.setSocketConfigurator)
    _exceptionHandler.foreach(cf.setExceptionHandler)
    _topologyRecovery.foreach(cf.setTopologyRecoveryEnabled)
    //    _networkRecoveryInterval.foreach(cf.setNetworkRecoveryInterval)

    _reconnectionStrategy match {
      case JavaClientFixedReconnectDelay(networkRecoveryInterval) ⇒
        cf.setAutomaticRecoveryEnabled(true)
        cf.setNetworkRecoveryInterval(networkRecoveryInterval.toMillis)
      case NoReconnect ⇒
        cf.setAutomaticRecoveryEnabled(false)
    }
    cf
  }

  def build(): ConnectionHolder = {
    val cf: ConnectionFactory = buildConnectionFactory

    val connection = cf.newConnection()
    createConnectionHolder(connection)
  }

  protected def createConnectionHolder(conn: Connection): ConnectionWrapper
}
