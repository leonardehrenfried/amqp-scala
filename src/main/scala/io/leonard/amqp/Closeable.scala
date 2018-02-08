package io.leonard.amqp

/** Closeable for a handler of RPCs, close to stop the handler from being called */
trait Closeable {
  def close(): Unit
}
