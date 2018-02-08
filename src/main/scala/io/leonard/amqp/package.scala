package io.leonard

/**
 * Scala wrapper for interacting with AMQP, the aim is for convenient:
 * - scala style usage
 * - RPC calls
 * - event hooks
 * - reconnection strategies
 * - message creation and extraction for common message types
 *
 * == Overview ==
 * Build connections with [[io.leonard.amqp.ConnectionHolder.builder]]
 *
 * Create a connection:
 * {{{
 * val connection = ConnectionHolder.builder("amqps://guest:password@host:port")
 *   .eventHooks(EventHooks(eventListener))
 *   .reconnectionStrategy(ReconnectionStrategy.JavaClientFixedReconnectDelay(1 second))
 *   .build()
 * }}}
 *
 * Create a channel:
 * {{{
 * val channel = connection.newChannel()
 * }}}
 *
 * Create an RPC server listening on queue "queue.name", expecting a String and echoing it back:
 * {{{
 * def rpcHandler(request: Message): Future[Message] = request match {
 *   case Message.String(string) => Future(Message.String(string))
 * }
 * val queue = QueueDeclare(Some("queue.name"))
 * val rpcServerCloser = channel.rpcServer(queue, AckOnHandled)(rpcHandler)
 * }}}
 *
 * Create an RPC client method which sends requests to the queue "queue.name" with a response timeout of 10 seconds :
 * {{{
 * val rpcClient = RPCClient(channel)
 * val rpcMethod = rpcClient.newMethod(Exchange.Default.route("queue.name"), 10 second)
 * }}}
 *
 * Create a consumer on "queue.name" printing out strings sent to it:
 * {{{
 * def consumer(request: Message): Unit = request match {
 *   case Message.String(string) => println(string)
 * }
 * val queue = QueueDeclare(Some("queue.name"))
 * channel.addConsumer(queue, consumer)
 * }}}
 *
 * Send a message to "queue.name":
 * {{{
 * channel.send(Exchange.Default.route("queue.name"), Message.String("message")
 * }}}
 */
package object amqp
