package io.relayr.amqp.connection

import java.util.concurrent.Executor

import com.rabbitmq.client.ConnectionFactory
import amqptest.EmbeddedAMQPBroker
import io.relayr.amqp.{ChannelOwner, EventHooks}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
 * Tests that a connection can be set up to broker by the wrapper.
 * Probably only temporary until ReconnectingConnectionHolder is unit tested the rest of the stuff is available to run a complete integration test
 */
class ConnectionOnlyIntegrationSpec extends FlatSpec with Matchers with BeforeAndAfterEach with EmbeddedAMQPBroker {

  override def beforeEach() {
    initializeBroker()
  }

  val synchronousExecutor: ExecutionContextExecutor = ExecutionContext.fromExecutor(new Executor() {
    override def execute(command: Runnable): Unit = command.run()
  })


  "ReconnectingConnectionHolder" should "be able to produce a working channel" in {
    val factory = new ConnectionFactory()
    factory.setUri(amqpUri)
    factory.useSslProtocol()

    var sessionProvider: ChannelSessionProvider = null
    var executionContext: ExecutionContext = null
    def channelFactory(cs: ChannelSessionProvider, ec: ExecutionContext): ChannelOwner = {
      sessionProvider = cs
      executionContext = ec
      null: ChannelOwner // It wont be used anyway
    }

    val connectionHolder = new ReconnectingConnectionHolder(factory, None, EventHooks(), synchronousExecutor, channelFactory)

    connectionHolder.newChannel(0) // we wont keep the channel as we are pretending to be the channel at the moment
    sessionProvider.withChannel(channel ⇒
      channel.isOpen should be (true)
    )
  }

  override def afterEach() = {
    shutdownBroker()
  }
}
