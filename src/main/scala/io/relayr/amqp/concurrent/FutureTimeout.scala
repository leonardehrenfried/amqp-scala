package io.relayr.amqp.concurrent

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Future, TimeoutException }

object FutureTimeout {
  /**
   * Returns a new future which will be completed either by the original future or by a timeout exception after the
   * specified delay. Timeout tasks are cancelled when the main future completes
   * @param duration to set timeout
   * @param future for the main completion of the returned future
   * @param executor schedules the timeout and combines the futures
   */
  def timeoutAfter[T](duration: FiniteDuration)(future: Future[T])(implicit executor: ScheduledExecutor): Future[T] = {
    implicit val executionContext = executor.executionContext
    val timeoutFuture = scheduleTimeout(duration, executor)
    future.onComplete(cancelCallback(timeoutFuture))
    Future.firstCompletedOf(Seq(future, timeoutFuture))
  }

  private def cancelCallback[T](cancellableFuture: CancellableFuture[T]): (Any) ⇒ Boolean = {
    _ ⇒ cancellableFuture.cancel(mayInterruptIfRunning = false)
  }

  private def scheduleTimeout[T](duration: FiniteDuration, executor: ScheduledExecutor): CancellableFuture[Nothing] = {
    executor.delayExecution(throw new TimeoutException("Future timed out"))(duration)
  }
}

