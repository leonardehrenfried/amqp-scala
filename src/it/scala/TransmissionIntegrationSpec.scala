import amqptest.AMQPIntegrationFixtures
import io.relayr.amqp.Event.ChannelEvent
import io.relayr.amqp._
import org.scalatest.{FlatSpec, Matchers}

import scala.language.postfixOps
import scala.concurrent.duration._

class TransmissionIntegrationSpec extends FlatSpec with Matchers with AMQPIntegrationFixtures {
  
  val testMessage: Message = Message.String("test")

  "" should "send and receive messages" in new ClientTestContext with ServerTestContext {
    // create server connection and bind mock handler to queue
    val receiver = mockFunction[Message, Unit]
    val serverCloser = {
      serverEventListener expects ChannelEvent.ChannelOpened(1, None)

      val serverChannel: ChannelOwner = serverConnection.newChannel()
      val queue: QueuePassive = QueuePassive(serverChannel.declareQueue(QueueDeclare(Some("test.queue"))))
      val exchange = serverChannel.declareExchange("test-exchange", ExchangeType.topic, false, false)
      serverChannel.queueBind(queue, exchange, "test.key")
      serverChannel.addConsumer(queue, receiver)
    }

    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()
    val exchange = senderChannel.declareExchange("test-exchange", ExchangeType.topic, false, false)
    val destinationDescriptor = exchange.route("test.key", DeliveryMode.NotPersistent, true, true)

    // define expectations
    receiver expects * onCall { message: Message ⇒
      ()
    }

    // send message
    senderChannel.sendPublish(Publish(destinationDescriptor, testMessage))
    
    Thread.sleep(1000)
    
    serverCloser.close()
  }

  "mandatory message to non-existent queue" should "be returned" in new ClientTestContext {
    val onReturn = mockFunction[Unit]
    
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
    onReturn expects()
    senderChannel.send(Exchange.Default.route("non.existent.queue", mandatory = true, immediate = false), testMessage, onReturn, 5 seconds)

    Thread.sleep(1000)
  }

  "immediate message to non-existent queue" should "be returned" in new ClientTestContext {
    val onReturn = mockFunction[Unit]
    
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
    onReturn expects()
    senderChannel.send(Exchange.Default.route("non.existent.queue", mandatory = false, immediate = true), testMessage, onReturn, 5 seconds)

    Thread.sleep(1000)
  }

  "mandatory message to non-consumed queue" should "not be returned" in new ClientTestContext {
    val onReturn = mockFunction[Unit]
    
    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()

    senderChannel.declareQueue(QueueDeclare(Some("non.consumed.queue")))
    senderChannel.send(Exchange.Default.route("non.consumed.queue", mandatory = true, immediate = false), testMessage, onReturn, 5 seconds)

    Thread.sleep(1000)
  }

  "immediate message to non-consumed queue" should "be returned" in new ClientTestContext {
    val onReturn = mockFunction[Unit]
    
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
    onReturn expects()
    senderChannel.send(Exchange.Default.route("non.consumed.queue", mandatory = false, immediate = true), testMessage, onReturn, 5 seconds)

    Thread.sleep(1000)
  }

  "declaring the same queue twice" should "be fine" in new ClientTestContext {
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()

    val newQueueDeclare: QueueDeclare = QueueDeclare(Some("new.queue"))
    senderChannel.declareQueue(newQueueDeclare)
    senderChannel.declareQueue(newQueueDeclare)
  }

  "declaring the same exchange twice" should "be fine" in new ClientTestContext {
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()

    senderChannel.declareExchange("test.exchange", ExchangeType.topic, false, false)
    senderChannel.declareExchange("test.exchange", ExchangeType.topic, false, false)
  }
}
