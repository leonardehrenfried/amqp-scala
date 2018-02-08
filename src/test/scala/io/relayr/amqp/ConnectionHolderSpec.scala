package io.leonard.amqp

import java.util.concurrent.{ ThreadFactory, ExecutorService }
import javax.net.SocketFactory

import com.rabbitmq.client.{ SocketConfigurator, SaslConfig, ExceptionHandler }
import io.leonard.amqp.ReconnectionStrategy.NoReconnect
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class ConnectionHolderSpec extends FlatSpec with Matchers with MockFactory {

  "ConnectionHolder.Builder" should "be constructable from builder methods" in {
    val URI = "uri"
    val AUTOMATIC_RECOVERY = true
    val CLIENT_PROPERTIES = Map("key" -> "value")
    val CONNECTION_TIMEOUT = 3
    val EVENT_HOOKS = mock[EventHooks]
    val EXCEPTION_HANDLER = mock[ExceptionHandler]
    val NETWORK_RECOVERY_INTERVAL = 4
    val RECONNECTION_STRATEGY = NoReconnect
    val CHANNEL_MAX = 5
    val FRAME_MAX = 6
    val HEARTBEAT = 7
    val SASL_CONFIG = mock[SaslConfig]
    val SHARED_EXECUTOR = mock[ExecutorService]
    val SHUTDOWN_TIMEOUT = 8
    val SOCKET_CONFIGURATOR = mock[SocketConfigurator]
    val SOCKET_FACTORY = mock[SocketFactory]
    val THREAD_FACTORY = mock[ThreadFactory]
    val TOPOLOGY_RECOVERY = true

    val ch = ConnectionHolder.Builder(URI)
      //      .automaticRecovery(AUTOMATIC_RECOVERY)
      .clientProperties(CLIENT_PROPERTIES)
      .connectionTimeout(CONNECTION_TIMEOUT)
      .eventHooks(EVENT_HOOKS)
      .exceptionHandler(EXCEPTION_HANDLER)
      //      .networkRecoveryInterval(NETWORK_RECOVERY_INTERVAL)
      .reconnectionStrategy(RECONNECTION_STRATEGY)
      .requestedChannelMax(CHANNEL_MAX)
      .requestedFrameMax(FRAME_MAX)
      .requestedHeartbeat(HEARTBEAT)
      //      .saslConfig(SASL_CONFIG)
      .sharedExecutor(SHARED_EXECUTOR)
      .shutdownTimeout(SHUTDOWN_TIMEOUT)
      .socketConfigurator(SOCKET_CONFIGURATOR)
      //      .socketFactory(SOCKET_FACTORY)
      .threadFactory(THREAD_FACTORY)
      .topologyRecovery(TOPOLOGY_RECOVERY)

    //    ch._automaticRecovery should be (Some(AUTOMATIC_RECOVERY))
    ch._clientProperties should be (CLIENT_PROPERTIES)
    ch._connectionTimeout should be (Some(CONNECTION_TIMEOUT))
    ch._eventHooks should be (EVENT_HOOKS)
    ch._exceptionHandler should be (Some(EXCEPTION_HANDLER))
    //    ch._networkRecoveryInterval should be (Some(NETWORK_RECOVERY_INTERVAL))
    ch._reconnectionStrategy should be (RECONNECTION_STRATEGY)
    ch._requestedChannelMax should be (Some(CHANNEL_MAX))
    ch._requestedFrameMax should be (Some(FRAME_MAX))
    ch._requestedHeartbeat should be (Some(HEARTBEAT))
    //    ch._saslConfig should be (Some(SASL_CONFIG))
    ch._sharedExecutor should be (Some(SHARED_EXECUTOR))
    ch._shutdownTimeout should be (Some(SHUTDOWN_TIMEOUT))
    ch._socketConfigurator should be (Some(SOCKET_CONFIGURATOR))
    //    ch._socketFactory should be (Some(SOCKET_FACTORY))
    ch._threadFactory should be (Some(THREAD_FACTORY))
    ch._topologyRecovery should be (Some(TOPOLOGY_RECOVERY))
    ch._uri should be (URI)
  }
}
