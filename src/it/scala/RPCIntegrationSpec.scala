import amqptest.EmbeddedAMQPBroker
import com.rabbitmq.client.ConnectionFactory
import io.relayr.amqp._
import io.relayr.amqp.rpc.client._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class RPCIntegrationSpec  extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with EmbeddedAMQPBroker with MockFactory {

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

  "" should "make and fulfill RPCs" in {
    // create server connection and bind mock handler to queue
    val rpcHandler = mockFunction[Message, Future[Message]]
    val rpcServer = {
      val queue: QueueDeclare = QueueDeclare(Some("test.queue"))
      serverConnection.newChannel(0).rpcServer(queue)(rpcHandler)(ExecutionContext.global)
    }

    // create client connection and bind to routing key
    val rpcClient = RPCClient(clientConnection.newChannel(0))
    val rpcDescriptor = ExchangePassive("").route("test.queue", DeliveryMode.NotPersistent)
    val rpcMethod = rpcClient.newMethod(rpcDescriptor, 10 second)

    // define expectations
    rpcHandler expects testMessage onCall { message: Message ⇒
      Future.successful(testMessage)
    }

    // make RPC
    val rpcResultFuture: Future[Message] = rpcMethod(testMessage)
    val result: Message = Await.result(rpcResultFuture, atMost = 10 second)
    
    // check response
    result should be (testMessage)
    
    // stop the rpc server, detaching it from the queue
    rpcServer.close()
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
