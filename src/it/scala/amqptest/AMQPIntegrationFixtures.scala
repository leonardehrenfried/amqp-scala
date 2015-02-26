package amqptest

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

    clientEventListener expects *

    val clientConnection: ConnectionHolder = connection(clientEventListener)
  }

  trait ServerTestContext {
    val serverEventListener = mockFunction[Event, Unit]

    serverEventListener expects * // connection established event

    val serverConnection: ConnectionHolder = connection(serverEventListener)
  }



  override def afterAll() = {
    shutdownBroker()
  }
}
