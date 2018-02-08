package io.leonard.amqp.connection

import com.rabbitmq.client.{ ShutdownListener, ShutdownSignalException }

object Listeners {

  def shutdownListener(exec: ShutdownSignalException ⇒ Unit): ShutdownListener = new ShutdownListener {
    override def shutdownCompleted(cause: ShutdownSignalException): Unit = {
      exec(cause)
    }
  }
}
