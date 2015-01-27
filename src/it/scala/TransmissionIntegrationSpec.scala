import amqptest.EmbeddedAMQPBroker
import com.rabbitmq.client.ConnectionFactory
import io.relayr.amqp._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

class TransmissionIntegrationSpec  extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with EmbeddedAMQPBroker with MockFactory {

  override def beforeAll() {
    initializeBroker()
  }

  val chf = {
    val factory = new ConnectionFactory()
    factory.setUri(amqpUri)
    factory.useSslProtocol()
    ConnectionHolderBuilder(factory, ExecutionContext.global)
  }
  
  var serverConnection: ConnectionHolder = null
  var clientConnection: ConnectionHolder = null

  override def beforeEach() = {
    serverConnection = chf.newConnectionHolder()
    clientConnection = chf.newConnectionHolder()
  }
  
  val testMessage: Message = Message("type", "encoding", ByteArray("test".getBytes))

  "" should "send and receive messages" in {
    // create server connection and bind mock handler to queue
    val receiver = mockFunction[Delivery, Unit]
    val rpcServer = {
      val queue: QueueDeclare = QueueDeclare(Some("test.queue"))
      serverConnection.newChannel(0).addConsumer(queue, true, receiver)
    }

    // create client connection and bind to routing key
    val senderChannel: ChannelOwner = clientConnection.newChannel(0)
    val destinationDescriptor = ExchangePassive("").route("test.queue", DeliveryMode.NotPersistent)

    // define expectations
    receiver expects * onCall { message: Delivery ⇒
      println("SHIT")
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
