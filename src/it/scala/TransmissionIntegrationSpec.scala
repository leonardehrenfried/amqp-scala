import amqptest.EmbeddedAMQPBroker
import com.rabbitmq.client.ConnectionFactory
import io.relayr.amqp.Event.ChannelEvent
import io.relayr.amqp._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

class TransmissionIntegrationSpec  extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with EmbeddedAMQPBroker with MockFactory {

  override def beforeAll() {
    initializeBroker()
  }

  def connection(eventListener: Event ⇒ Unit) = {
    val factory = new ConnectionFactory()
    factory.setUri(amqpUri)
    factory.useSslProtocol()
    ConnectionHolder.Builder(factory, ExecutionContext.global, eventHooks = EventHooks(eventListener))
      .newConnectionHolder()
  }

  val serverEventListener = mockFunction[Event, Unit]
  val clientEventListener = mockFunction[Event, Unit]
  
  var serverConnection: ConnectionHolder = null
  var clientConnection: ConnectionHolder = null

  override def beforeEach() = {
    serverEventListener expects * // connection established event
    clientEventListener expects *
    
    serverConnection = connection(serverEventListener)
    clientConnection = connection(clientEventListener)
  }
  
  val testMessage: Message = Message("type", "encoding", ByteArray("test".getBytes))

  "" should "send and receive messages" in {
    // create server connection and bind mock handler to queue
    val receiver = mockFunction[Delivery, Unit]
    val rpcServer = {
      serverEventListener expects ChannelEvent.ChannelOpened(1, None)
      val queue: QueueDeclare = QueueDeclare(Some("test.queue"))
      serverConnection.newChannel().addConsumer(queue, true, receiver)
    }

    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()
    val destinationDescriptor = ExchangePassive("").route("test.queue", DeliveryMode.NotPersistent)

    // define expectations
    receiver expects * onCall { message: Delivery ⇒
      ()
    }

    // send message
    senderChannel.send(destinationDescriptor, testMessage)
    
    Thread.sleep(1000)
  }
  
  override def afterEach() = {
    // close
    serverConnection.close()
    clientConnection.close()
  }

  override def afterAll() = {
    shutdownBroker()
  }
}
