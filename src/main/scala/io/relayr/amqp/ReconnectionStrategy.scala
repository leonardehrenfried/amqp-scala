package io.relayr.amqp

import scala.concurrent.duration.FiniteDuration

private[amqp] object ReconnectionStrategy {
  val default: Stream[FiniteDuration] = ???
}
