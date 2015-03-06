package amqptest

import java.net.InetAddress

import io.relayr.amqp.Event.ConnectionEvent.ConnectionEstablished
import io.relayr.amqp.{ReconnectionStrategy, EventHooks, ConnectionHolder, Event}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Suite, BeforeAndAfterAll}

import scala.concurrent.duration._
import scala.language.postfixOps

trait AMQPIntegrationFixtures extends BeforeAndAfterAll with EmbeddedAMQPBroker with MockFactory { this: Suite =>

  override def beforeAll() {
    initializeBroker()
  }

  def connection(eventListener: Event ⇒ Unit) = ConnectionHolder.builder(amqpUri)
    .eventHooks(EventHooks(eventListener))
    .reconnectionStrategy(ReconnectionStrategy.JavaClientFixedReconnectDelay(1 second))
    .build()

  trait ClientTestContext {
    val clientEventListener = mockFunction[Event, Unit]

    clientEventListener expects ConnectionEstablished(InetAddress.getByName("localhost"),brokerAmqpPort,0 seconds)

    val clientConnection: ConnectionHolder = connection(clientEventListener)
  }

  trait ServerTestContext {
    val serverEventListener = mockFunction[Event, Unit]

    serverEventListener expects ConnectionEstablished(InetAddress.getByName("localhost"),brokerAmqpPort,0 seconds) // connection established event

    val serverConnection: ConnectionHolder = connection(serverEventListener)
  }



  override def afterAll() = {
    shutdownBroker()
  }
}
