package io.relayr.amqp

trait ManualAcker {
  def ack(): Unit
  def reject(requeue: Boolean): Unit
}
