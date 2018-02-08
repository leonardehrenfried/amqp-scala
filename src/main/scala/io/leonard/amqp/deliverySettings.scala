package io.leonard.amqp

/**
 * DeliveryMode is a header on AMQP messages. It ONLY has an effect on messages which arrive at DURABLE queues.
 *
 * DURABLE queues are ones which are persistent themselves - they are recreated after a broker restart.
 *
 * Persistent messages on DURABLE queues are also written to disk and will be recovered after a broker restart.
 */
object DeliveryMode {

  def apply(value: Int) = value match {
    case 1 ⇒ NotPersistent
    case 2 ⇒ Persistent
  }

  case object NotPersistent extends DeliveryMode(1)

  case object Persistent extends DeliveryMode(2)

}

sealed abstract class DeliveryMode(val value: Int)

/**
 * Some options for when the server will Ack the request message, clients only have the AckOnReceive auto Ack strategy
 */
object RpcServerAutoAckMode {

  /** DEFAULT : Client automatically acknowledges when it receives the message */
  case object AckOnReceive extends RpcServerAutoAckMode

  /** (RPC Server only) ack on return of the handler, when the Future is returned but could be before the Future completes. Nack is sent in the event of an exception */
  case object AckOnHandled extends RpcServerAutoAckMode

  /** (RPC Server only) ack on successful completion of the handler future, a failure causes a NAck  */
  case object AckOnSuccessfulResponse extends RpcServerAutoAckMode

}

sealed trait RpcServerAutoAckMode
