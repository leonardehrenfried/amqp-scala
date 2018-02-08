package io.leonard.amqp.rpc.server

import java.util.concurrent.Executor

import io.leonard.amqp.DeliveryMode.NotPersistent
import io.leonard.amqp.Event.HandlerError
import io.leonard.amqp.RpcServerAutoAckMode.AckOnReceive
import io.leonard.amqp._
import io.leonard.amqp.properties.Key.{ CorrelationId, ReplyTo }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future, Promise }

class RPCServerSpec extends FlatSpec with Matchers with MockFactory {
  val channelOwner = mock[ChannelOwner]
  val handler = mockFunction[Message, Future[Message]]
  val queue = QueuePassive("queue name")
  val replyChannel = "reply channel"
  val correlationId = "correlation id"
  val eventHandler = mockFunction[Event, Unit]

  val msg: Message = Message.String("string").withProperties(
    CorrelationId → correlationId,
    ReplyTo → replyChannel)

  implicit val synchronousExecutor: ExecutionContextExecutor = ExecutionContext.fromExecutor(new Executor() {
    override def execute(command: Runnable): Unit = command.run()
  })

  def createRpcServer(ackMode: RpcServerAutoAckMode): (RPCServerImpl, (Message, ManualAcker) ⇒ Unit) = {
    var consumer: (Message, ManualAcker) ⇒ Unit = null
    (channelOwner.addConsumerAckManual(_: Queue, _: (Message, ManualAcker) ⇒ Unit)) expects (queue, *) onCall { (_, _consumer) ⇒
      consumer = _consumer
      mock[Closeable]
    }
    val rpcServer = new RPCServerImpl(channelOwner, queue, ackMode, eventHandler, synchronousExecutor, handler, ResponseParameters(false, false, Some(NotPersistent)))
    (rpcServer, consumer)
  }

  "RPCServer" should "register a consumer" in {
    (channelOwner.addConsumerAckManual(_: Queue, _: (Message, ManualAcker) ⇒ Unit)) expects (queue, *)
    new RPCServerImpl(channelOwner, queue, AckOnReceive, eventHandler, synchronousExecutor, handler, ResponseParameters(false, false, None))
  }

  it should "call the handler when a message comes in on the listener and correctly reply with the response" in {
    val (rpcServer, consumer) = createRpcServer(RpcServerAutoAckMode.AckOnHandled)

    // when a message is delivered it should trigger the handler
    val acker = mock[ManualAcker]
    val resultPromise = Promise[Message]()
    inOrder (
      handler expects msg returning resultPromise.future,
      acker.ack _ expects ()
    )
    consumer(msg, acker)

    // when the handler's result is completed, the reply shouldbe sent
    (channelOwner.send(_: RoutingDescriptor, _: Message)) expects (Exchange.Default.route(replyChannel, DeliveryMode.NotPersistent), *) onCall {
      (RoutingDescriptor, m: Message) ⇒
        val Message.String(string) = m
        assert(string == "string")
        assert(m.property(CorrelationId).equals(Some(correlationId)) && m.property(ReplyTo).equals(Some(replyChannel)))
    }
    resultPromise.success(msg)
  }

  it should "be able to ack on receive" in {
    val (rpcServer, consumer) = createRpcServer(RpcServerAutoAckMode.AckOnReceive)

    // when a message is delivered it should trigger the handler
    val acker = mock[ManualAcker]
    val resultPromise = Promise[Message]()
    inSequence {
      acker.ack _ expects ()
      handler expects msg returning resultPromise.future
    }
    consumer(msg, acker)

    // when the handler's result is completed, the reply should be sent
    (channelOwner.send(_: RoutingDescriptor, _: Message)) expects (*, *)
    resultPromise.success(msg)
  }

  it should "be able to ack on handled" in {
    val (rpcServer, consumer) = createRpcServer(RpcServerAutoAckMode.AckOnHandled)

    // when a message is delivered it should trigger the handler
    val acker = mock[ManualAcker]
    val resultPromise = Promise[Message]()
    inSequence {
      handler expects msg returning resultPromise.future
      acker.ack _ expects ()
    }
    consumer(msg, acker)

    // when the handler's result is completed, the reply should be sent
    (channelOwner.send(_: RoutingDescriptor, _: Message)) expects (*, *)
    resultPromise.success(msg)
  }

  it should "be able to ack on success" in {
    val (rpcServer, consumer) = createRpcServer(RpcServerAutoAckMode.AckOnSuccessfulResponse)

    // when a message is delivered it should trigger the handler
    val acker = mock[ManualAcker]
    val resultPromise = Promise[Message]()
    handler expects msg returning resultPromise.future
    consumer(msg, acker)

    // when the handler's result is completed, the reply should be sent
    inSequence {
      acker.ack _ expects ()
      (channelOwner.send(_: RoutingDescriptor, _: Message)) expects (*, *)
      resultPromise.success(msg)
    }
  }

  it should "report errors and reject message for AckOnSuccessfulResponse on failed Future" in {
    val (rpcServer, consumer) = createRpcServer(RpcServerAutoAckMode.AckOnSuccessfulResponse)

    // when a message is delivered it should trigger the handler
    val acker = mock[ManualAcker]
    val resultPromise = Promise[Message]()
    handler expects msg returning resultPromise.future
    consumer(msg, acker)

    val exception: Exception = new scala.Exception
    eventHandler expects HandlerError(exception)
    acker.reject _ expects false
    resultPromise.failure(exception)
  }

  it should "report errors and reject message for AckOnHandled on handler exception thrown" in {
    val (rpcServer, consumer) = createRpcServer(RpcServerAutoAckMode.AckOnHandled)

    val acker = mock[ManualAcker]
    val exception: Exception = new scala.Exception
    handler expects msg throwing exception

    eventHandler expects HandlerError(exception)
    acker.reject _ expects false
    consumer(msg, acker)
  }

  it should "report errors for AckOnReceive on failed future" in {
    val (rpcServer, consumer) = createRpcServer(RpcServerAutoAckMode.AckOnReceive)

    // when a message is delivered it should trigger the handler
    val acker = mock[ManualAcker]
    val resultPromise = Promise[Message]()
    inSequence {
      acker.ack _ expects ()
      handler expects msg returning resultPromise.future
    }
    consumer(msg, acker)

    val exception: Exception = new scala.Exception
    eventHandler expects HandlerError(exception)
    resultPromise.failure(exception)
  }

  it should "report errors for AckOnReceive on handler exception thrown" in {
    val (rpcServer, consumer) = createRpcServer(RpcServerAutoAckMode.AckOnReceive)

    // when a message is delivered it should trigger the handler
    val acker = mock[ManualAcker]
    val resultPromise = Promise[Message]()
    val exception: Exception = new scala.Exception
    inSequence {
      acker.ack _ expects ()
      handler expects msg throwing exception
    }

    eventHandler expects HandlerError(exception)
    consumer(msg, acker)
  }

  it should "create a new queue if given a queue declare"
}
