package io.relayr.amqp

import java.util.concurrent.Executor

import amqptest.EmbeddedAMQPBroker
import com.rabbitmq.client.ConnectionFactory
import io.relayr.amqp.rpc.client._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, FlatSpec, Matchers}
import scala.concurrent.duration._

import scala.concurrent.{Await, ExecutionContextExecutor, Future, ExecutionContext}

class RPCIntegrationSpec  extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with EmbeddedAMQPBroker with MockFactory {

  override def beforeAll() {
    initializeBroker()
  }

  val synchronousExecutor: ExecutionContextExecutor = ExecutionContext.fromExecutor(new Executor() {
    override def execute(command: Runnable): Unit = command.run()
  })

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

  ignore should "make and fulfill RPCs" in {
    // create server connection and bind mock handler to queue
    val rpcHandler = mockFunction[Message, Future[Message]]
    val rpcServer = {
      val exchange: ExchangePassive = ExchangePassive("")
      val queue: QueueDeclare = QueueDeclare("test.queue")
      val routingKey: String = "routingKey not used as this is direct"
      val rpcQueueBinding = Binding(exchange, queue, routingKey)
      serverConnection.newChannel(0).rpcServer(rpcQueueBinding)(rpcHandler)(synchronousExecutor)
    }

    // create client connection and bind to routing key
    val rpcClient = RPCClient(clientConnection.newChannel(0))
    val rpcDescriptor = Exchange.Direct.route("test.queue", DeliveryMode.NotPersistent)
    val rpcMethod = rpcClient(rpcDescriptor, 1 second)

    // define expectations
    rpcHandler expects testMessage returning Future.successful(testMessage)

    // make RPC
    val rpcResultFuture: Future[Message] = rpcMethod(testMessage)
    val result: Message = Await.result(rpcResultFuture, atMost = 1 second)
    
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
