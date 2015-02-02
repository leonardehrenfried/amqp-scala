package io.relayr.amqp.concurrent

import java.util.concurrent.TimeoutException

import org.scalatest.{ Matchers, FlatSpec }

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class ScheduledExecutorSpec extends FlatSpec with Matchers {

  val executor = new ScheduledExecutor(1)

  "Scheduled operations" should "not complete too early" in {
    intercept[TimeoutException](Await.ready(executor.delayExecution(())(50 milliseconds), 10 milliseconds))
  }

  it should "not take too long" in {
    val execution: Future[Unit] = executor.delayExecution(())(10 milliseconds)
    val ready: Future[Unit] = Await.ready(execution, 50 milliseconds)
    ready.isCompleted should be (true)
  }

  it should "complete in order" in {
    val future = Future.firstCompletedOf(Seq[Future[String]](
      executor.delayExecution("short")(10 milliseconds),
      executor.delayExecution("long")(50 milliseconds)))
    Await.result(future, 100 millis) should be ("short")
  }

  it should "complete out of order" in {
    val future = Future.firstCompletedOf(Seq[Future[String]](
      executor.delayExecution("long")(50 milliseconds),
      executor.delayExecution("short")(10 milliseconds)))
    Await.result(future, 100 millis) should be ("short")
  }

  it should "not complete after cancellation" in {
    val execution = executor.delayExecution(())(10 milliseconds)
    execution.cancel(false)
    intercept[TimeoutException](Await.ready(execution, 50 milliseconds))
  }
}
