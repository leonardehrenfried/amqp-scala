package io.leonard.amqp.concurrent

import java.util.concurrent.TimeoutException

import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class FutureTimeoutSpec extends FlatSpec with Matchers {

  implicit val executor = new ScheduledExecutor(1)

  "FutureTimeout" should "allow a quick enough operation to complete" in {
    val future = FutureTimeout.timeoutAfter(20 millis)(executor.delayExecution("short")(10 milliseconds))
    Await.result(future, 100 millis) should be ("short")
  }

  it should "timeout longer operations" in {
    val future = FutureTimeout.timeoutAfter(10 millis)(executor.delayExecution("long")(50 milliseconds))
    intercept[TimeoutException](Await.result(future, 100 millis))
  }
}
