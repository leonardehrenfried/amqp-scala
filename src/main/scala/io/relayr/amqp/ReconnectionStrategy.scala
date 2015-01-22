package io.relayr.amqp

import scala.concurrent.duration.FiniteDuration

object ReconnectionStrategy {
  val default: Stream[FiniteDuration] = ???
}
