import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.{SynchronousQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit}

import amqptest.AMQPIntegrationFixtures
import io.relayr.amqp.Event.ChannelEvent
import io.relayr.amqp.RpcServerAutoAckMode.AckOnHandled
import io.relayr.amqp._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class RPCConcurrentCallsSpec extends FlatSpec with Matchers with AMQPIntegrationFixtures {

  val threads = 10
  val concurrentCalls = 10

  implicit val ec: ExecutionContext = createIOExectionContext

  "RPCClient" should "make and fulfill " + concurrentCalls + " concurrent RPCs each over " + threads + " threads" in new ClientTestContext with ServerTestContext {
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
    } repeated (concurrentCalls * threads) times

    val rpcCallNumbers = Range.inclusive(1, concurrentCalls)
    val threadNumbers = Range.inclusive(1, threads)

    def testAThread() = Future {
      // make RPC
      val rpcResultFutures = rpcCallNumbers.map(requestNo => rpcMethod(Message.String("request " + requestNo)))
      val responses: IndexedSeq[Message] = Await.result(Future.sequence(rpcResultFutures), atMost = 10 seconds)

      rpcCallNumbers.foreach { requestNo =>
        val message = responses(requestNo - 1)
        val Message.String(string) = message
        string should be("reply to request " + requestNo)
      }
    }

    Await.result(Future.sequence(threadNumbers.map(_ => testAThread())), 20 seconds)

    // stop the rpc server, detaching it from the queue
    rpcServer.close()
  }

  // Scala 2.12 changed the global thread pool to be based on the one included in Java 8
  // this seems to cause deadlocks so create an unbound 'io' thread pool to use instead
  private def createIOExectionContext: ExecutionContext = {
    val threadFactory = new ThreadFactory {
      def newThread(r: Runnable): Thread = {
        val thread = new Thread(r)
        thread.setName(s"test-${thread.getId}")
        thread.setDaemon(true)
        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler {
          override def uncaughtException(t: Thread, e: Throwable): Unit = e.printStackTrace()
        })
        thread
      }
    }

    val executor = new ThreadPoolExecutor(
      0, Int.MaxValue,
      60, TimeUnit.SECONDS,
      new SynchronousQueue[Runnable](false),
      threadFactory
    )

    ExecutionContext.fromExecutor(executor)
  }
}
