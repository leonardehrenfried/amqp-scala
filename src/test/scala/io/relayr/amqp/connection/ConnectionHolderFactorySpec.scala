package io.relayr.amqp.connection

import java.util.concurrent.{ ExecutorService, ThreadFactory }

import com.rabbitmq.client.{ Connection, ExceptionHandler, SocketConfigurator }
import io.relayr.amqp.ReconnectionStrategy.{ JavaClientFixedReconnectDelay, LyraRecoveryStrategy, NoReconnect }
import io.relayr.amqp.{ EventHooks, ReconnectionStrategy }
import net.jodah.lyra.config.Config
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.JavaConversions
import scala.concurrent.duration._
import scala.language.postfixOps

class ConnectionHolderFactorySpec extends FlatSpec with Matchers with MockFactory {

  trait FakeConnectionFactory extends ConnectionHolderFactory {
    override val _uri: String = "amqps://host:123"
    override val _topologyRecovery: Option[Boolean] = Some(true)
    //    override val _saslConfig: Option[SaslConfig] = None // Some(mock[SaslConfig])
    override val _requestedFrameMax: Option[Int] = Some(3)
    override val _exceptionHandler: Option[ExceptionHandler] = Some(mock[ExceptionHandler])
    override val _shutdownTimeout: Option[Int] = Some(4)
    override val _requestedHeartbeat: Option[Int] = Some(5)
    override val _clientProperties: Map[String, AnyRef] = Map("key" -> "value")
    //    override val _socketFactory: Option[SocketFactory] = None // Some(mock[SocketFactory])
    //    override val _networkRecoveryInterval: Option[Long] = Some(6)
    override val _socketConfigurator: Option[SocketConfigurator] = Some(mock[SocketConfigurator])
    override val _requestedChannelMax: Option[Int] = Some(7)
    override val _threadFactory: Option[ThreadFactory] = Some(mock[ThreadFactory])
    override val _connectionTimeout: Option[Int] = Some(8)
    override val _sharedExecutor: Option[ExecutorService] = Some(mock[ExecutorService])
    override val _reconnectionStrategy: ReconnectionStrategy = NoReconnect
    override val _eventHooks: EventHooks = mock[EventHooks]

    override protected def createConnectionHolder(conn: Connection): ConnectionWrapper = {
      null
    }
  }

  "ConnectionHolderFactory" should "build a ConnectionHolder" in new FakeConnectionFactory {
    val cf = buildConnectionFactory

    cf.isSSL should be (true)
    cf.getHost should be ("host")
    cf.getPort should be (123)
    cf.isTopologyRecoveryEnabled should be (true)
    //    cf.getSaslConfig should be (_saslConfig)
    cf.getRequestedFrameMax should be (_requestedFrameMax.get)
    cf.getExceptionHandler should be (_exceptionHandler.get)
    cf.getShutdownTimeout should be (_shutdownTimeout.get)
    cf.getRequestedHeartbeat should be (_requestedHeartbeat.get)
    cf.getClientProperties should be (JavaConversions.mapAsJavaMap(_clientProperties))
    //    cf.getSocketFactory should be (_socketFactory)
    //    cf.getNetworkRecoveryInterval should be (_networkRecoveryInterval.get)
    cf.getSocketConfigurator should be (_socketConfigurator.get)
    cf.getRequestedChannelMax should be (_requestedChannelMax.get)
    cf.getThreadFactory should be (_threadFactory.get)
    cf.getConnectionTimeout should be (_connectionTimeout.get)
    //    cf.getSharedExecutor should be (_sharedExecutor)
  }

  it should "set no reconnection" in new FakeConnectionFactory {
    override val _reconnectionStrategy: ReconnectionStrategy = NoReconnect

    val cf = buildConnectionFactory

    cf.isAutomaticRecoveryEnabled should be (false)
  }

  it should "set reconnection" in new FakeConnectionFactory {
    override val _reconnectionStrategy: ReconnectionStrategy = JavaClientFixedReconnectDelay(8 seconds)

    val cf = buildConnectionFactory

    cf.isAutomaticRecoveryEnabled should be (true)
    cf.getNetworkRecoveryInterval should be ((8 seconds).toMillis)
  }

  it should "not enable automatic recovery for a lyra recovery strategy" in new FakeConnectionFactory {
    override val _reconnectionStrategy: ReconnectionStrategy = LyraRecoveryStrategy(new Config())

    val cf = buildConnectionFactory

    cf.isAutomaticRecoveryEnabled should be (false)
  }
}
