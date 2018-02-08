package io.leonard.amqp

/** Defines a queue to connect to or create */
sealed trait Queue

/** Describes an exchange which should already exist, an error is thrown if it does not */
case class QueuePassive(name: String) extends Queue

/** Parameters to create a new queue */
case class QueueDeclare(name: Option[String], durable: Boolean = false, exclusive: Boolean = false, autoDelete: Boolean = true, args: Map[String, AnyRef] = Map.empty) extends Queue
