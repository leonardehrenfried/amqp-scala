package io.relayr.amqp

object DeliveryMode {
  case object NotPersistent extends DeliveryMode(1)
  case object Persistent extends DeliveryMode(2)
}

sealed abstract class DeliveryMode(val value: Int)