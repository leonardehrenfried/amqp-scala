import amqptest.AMQPIntegrationFixtures
import io.relayr.amqp.Event.ChannelEvent
import io.relayr.amqp.RpcServerAutoAckMode.AckOnHandled
import io.relayr.amqp._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class RPCConcurrentCallsSpec extends FlatSpec with Matchers with AMQPIntegrationFixtures {
  
  val concurrentCalls = 10000

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
      string should startWith ("request")
      Future.successful(Message.String("reply to " + string))
    } repeated concurrentCalls times

    // make RPC
    val rpcResultFutures = Range.inclusive(1, concurrentCalls).map(requestNo => rpcMethod(Message.String("request")))
    Await.result(Future.sequence(rpcResultFutures), atMost = 10 seconds).foreach { message: Message =>
      val Message.String(string) = message
      string should startWith ("reply to request")
    }
    
    // stop the rpc server, detaching it from the queue
    rpcServer.close()
  }
}
