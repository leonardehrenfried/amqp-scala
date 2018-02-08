package io.leonard.amqp

/** Call to send Acks to Messages */
trait ManualAcker {
  def ack(): Unit
  def reject(requeue: Boolean): Unit
}
