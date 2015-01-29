package io.relayr.amqp.rpc.server

import java.util.concurrent.Executor

import io.relayr.amqp._
import io.relayr.amqp.properties.Key.{ CorrelationId, ReplyTo }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future, Promise }

class RPCServerSpec extends FlatSpec with Matchers with MockFactory {
  val channelOwner = mock[ChannelOwner]
  val handler = mockFunction[Message, Future[Message]]
  val queue = QueuePassive("queue name")

  implicit val synchronousExecutor: ExecutionContextExecutor = ExecutionContext.fromExecutor(new Executor() {
    override def execute(command: Runnable): Unit = command.run()
  })

  "RPCServer" should "register a consumer" in {
    (channelOwner.addConsumerAckManual(_: Queue, _: (Message, ManualAcker) ⇒ Unit)) expects (queue, *)
    new RPCServerImpl(channelOwner, queue, synchronousExecutor, handler)
  }

  it should "call the handler when a message comes in on the listener and correctly reply with the response" in {
    val replyChannel: String = "reply channel"
    val correlationId: String = "correlation id"
    var consumer: (Message, ManualAcker) ⇒ Unit = null
    (channelOwner.addConsumerAckManual(_: Queue, _: (Message, ManualAcker) ⇒ Unit)) expects (queue, *) onCall { (_, _consumer) ⇒
      consumer = _consumer
      mock[Closeable]
    }
    val rpcServer = new RPCServerImpl(channelOwner, queue, synchronousExecutor, handler)

    // when a message is relivered it should trigger the handler
    val manualAcker = mock[ManualAcker]
    val msg: Message = Message.String("string").withProperties(
      CorrelationId → correlationId,
      ReplyTo → replyChannel)
    val resultPromise = Promise[Message]()
    handler expects msg returning resultPromise.future
    manualAcker.ack _ expects ()
    consumer(msg, manualAcker)

    // when the handler's result is completed, the reply shouldbe sent
    channelOwner.send _ expects (Exchange.Default.route(replyChannel, DeliveryMode.NotPersistent), *) onCall {
      (RoutingDescriptor, m: Message) ⇒
        val Message.String(string) = m
        assert(string == "string")
        assert(m.property(CorrelationId).equals(Some(correlationId)) && m.property(ReplyTo).equals(Some(replyChannel)))
    }
    resultPromise.success(msg)
  }

  it should "create a new queue if given a queue declare"
}
