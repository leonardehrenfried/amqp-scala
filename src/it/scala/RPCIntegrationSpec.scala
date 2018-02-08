import amqptest.AMQPIntegrationFixtures
import io.leonard.amqp.Event.ChannelEvent
import io.leonard.amqp.RpcServerAutoAckMode.AckOnHandled
import io.leonard.amqp._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class RPCIntegrationSpec extends FlatSpec with Matchers with AMQPIntegrationFixtures {
  
  val testMessage: Message = Message.String("request")

  "RPCClient" should "make and fulfill RPCs" in new ClientTestContext with ServerTestContext {
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
  
  "RPCClient" should "throw an exception if the target queue does not exist" in new ClientTestContext {
    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val rpcClient = RPCClient(clientConnection.newChannel())
    val rpcDescriptor = Exchange.Default.route("non.existent.queue", mandatory = false, immediate = true)
    val rpcMethod: RPCMethod = rpcClient.newMethod(rpcDescriptor, 10 second)

    clientEventListener expects *    // make RPC
    val rpcResultFuture: Future[Message] = rpcMethod(testMessage)
    intercept[UndeliveredException](Await.result(rpcResultFuture, atMost = 10 second))
  }
}
