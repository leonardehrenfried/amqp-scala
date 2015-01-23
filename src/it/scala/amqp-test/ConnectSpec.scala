package io.relayr.amqp.test

import com.rabbitmq.client.ConnectionFactory
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}


/** This actually tests the test broker, all that and this stuff should be moved to a sub project */
class ConnectSpec extends FlatSpec with Matchers with BeforeAndAfterEach with EmbeddedAMQPBroker {

  override def beforeEach() {
    initializeBroker()
  }

  "Connect" should "work" in {
    val factory = new ConnectionFactory()
    factory.setUri(amqpUri)
    factory.useSslProtocol()

    def connection = factory.newConnection()
    //get a channel for sending the "kickoff" message
    def channel = connection.createChannel()
    channel.isOpen should be(true)
  }

  override def afterEach() = {
    shutdownBroker()
  }
}
