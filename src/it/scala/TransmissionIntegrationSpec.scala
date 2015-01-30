import amqptest.EmbeddedAMQPBroker
import io.relayr.amqp.Event.ChannelEvent
import io.relayr.amqp._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.duration._

class TransmissionIntegrationSpec  extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with EmbeddedAMQPBroker with MockFactory {

  override def beforeAll() {
    initializeBroker()
  }

  def connection(eventListener: Event ⇒ Unit) = ConnectionHolder.Builder(amqpUri)
    .eventHooks(EventHooks(eventListener))
    .reconnectionStrategy(ReconnectionStrategy.JavaClientFixedReconnectDelay(1 second))
    .build()


  class TestContext {
    val serverEventListener = mockFunction[Event, Unit]
    val clientEventListener = mockFunction[Event, Unit]
    
    serverEventListener expects * // connection established event
    clientEventListener expects *

    val serverConnection: ConnectionHolder = connection(serverEventListener)
    val clientConnection: ConnectionHolder = connection(clientEventListener)

    def closeConnections() = {
      // close
      serverConnection.close()
      clientConnection.close()
    }
  }
  
  val testMessage: Message = Message.String("test")

  "" should "send and receive messages" in new TestContext {
    // create server connection and bind mock handler to queue
    val receiver = mockFunction[Message, Unit]
    val serverCloser = {
      serverEventListener expects ChannelEvent.ChannelOpened(1, None)
      val queue: QueueDeclare = QueueDeclare(Some("test.queue"))
      serverConnection.newChannel().addConsumer(queue, receiver)
    }

    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()
    val destinationDescriptor = ExchangePassive("").route("test.queue", DeliveryMode.NotPersistent)

    // define expectations
    receiver expects * onCall { message: Message ⇒
      ()
    }

    // send message
    senderChannel.sendPublish(Publish(destinationDescriptor, testMessage))
    
    Thread.sleep(1000)
    
    serverCloser.close()
    closeConnections()
  }

  "mandatory message to non-existent queue" should "be returned" in new TestContext {
    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()

    clientEventListener expects where { (_: Event) match {
      case ChannelEvent.MessageReturned(
      312,
      "No Route for message [Exchange: null, Routing key: non.existent.queue]",
      "",
      "non.existent.queue",
      Message.String("test")) => true } }
    senderChannel.send(ExchangePassive("").route("non.existent.queue", mandatory = true, immediate = false), testMessage)

    Thread.sleep(1000)
  }

  "immediate message to non-existent queue" should "be returned" in new TestContext {
    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()

    clientEventListener expects where { (_: Event) match {
      case ChannelEvent.MessageReturned(
      312,
      "No Route for message [Exchange: null, Routing key: non.existent.queue]",
      "",
      "non.existent.queue",
      Message.String("test")) => true } }
    senderChannel.send(ExchangePassive("").route("non.existent.queue", mandatory = false, immediate = true), testMessage)

    Thread.sleep(1000)
  }

  "mandatory message to non-consumed queue" should "not be returned" in new TestContext {
    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()

    senderChannel.declareQueue(QueueDeclare(Some("non.consumed.queue")))
    senderChannel.send(ExchangePassive("").route("non.consumed.queue", mandatory = true, immediate = false), testMessage)

    Thread.sleep(1000)
  }

  "immediate message to non-consumed queue" should "be returned" in new TestContext {
    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()

    senderChannel.declareQueue(QueueDeclare(Some("non.consumed.queue")))
    clientEventListener expects where { (_: Event) match {
      case ChannelEvent.MessageReturned(
      313,
      "Immediate delivery is not possible.",
      "",
      "non.consumed.queue",
      Message.String("test")) => true } }
    senderChannel.send(ExchangePassive("").route("non.consumed.queue", mandatory = false, immediate = true), testMessage)

    Thread.sleep(1000)
  }
  

  override def afterAll() = {
    shutdownBroker()
  }
}
