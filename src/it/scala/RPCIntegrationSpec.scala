import amqptest.EmbeddedAMQPBroker
import io.relayr.amqp.Event.ChannelEvent
import io.relayr.amqp.RpcServerAutoAckMode.AckOnHandled
import io.relayr.amqp._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class RPCIntegrationSpec  extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with EmbeddedAMQPBroker with MockFactory {
  
  override def beforeAll() {
    initializeBroker()
  }

  def connection(eventListener: Event ⇒ Unit) = ConnectionHolder.Builder(amqpUri)
    .eventHooks(EventHooks(eventListener))
    .reconnectionStrategy(ReconnectionStrategy.JavaClientFixedReconnectDelay(1 second))
    .build()

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
  
  val testMessage: Message = Message.String("request")

  "" should "make and fulfill RPCs" in {
    // create server connection and bind mock handler to queue
    val rpcHandler = mockFunction[Message, Future[Message]]
    val rpcServer = {
      serverEventListener expects ChannelEvent.ChannelOpened(1, None)
      val queue: QueueDeclare = QueueDeclare(Some("test.queue"))
      serverConnection.newChannel().rpcServer(queue, AckOnHandled)(rpcHandler)
    }

    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val rpcClient = RPCClient(clientConnection.newChannel())
    val rpcDescriptor = Exchange.Default.route("test.queue", DeliveryMode.NotPersistent)
    val rpcMethod = rpcClient.newMethod(rpcDescriptor, 10 second)

    // define expectations
    rpcHandler expects * onCall { message: Message ⇒
      val Message.String(string) = message
      string should be ("request")
      Future.successful(Message.String("reply"))
    }

    // make RPC
    val rpcResultFuture: Future[Message] = rpcMethod(testMessage)
    val Message.String(string) = Await.result(rpcResultFuture, atMost = 10 second)

    // check response
    string should be ("reply")
    
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
