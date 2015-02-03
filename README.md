![Build status](https://travis-ci.org/relayr/rabbitmq-scala-client.svg?branch=master)
[![Coverage Status](https://coveralls.io/repos/relayr/rabbitmq-scala-client/badge.svg?branch=master)](https://coveralls.io/r/relayr/rabbitmq-scala-client?branch=master)

# rabbitmq-scala-client

Wrapper around the rabbitmq-java-client for better scala usage. Much of this is based on 
[amqp-client](https://github.com/sstone/amqp-client) by [sstone](https://github.com/sstone). 
The main reason for the rewrite is to not require our clients to use akka, to be easier to configure and to implement 
event hooks to enable statistics gathering.


## Requirements

1. We currently use the AMQP RPC pattern extensively, so Non-blocking RPC should be fairly easy
2. Configurable reconnection strategies - (exponential backoff for one)
3. We have changing logging and statistics requirements, so we need some solution for that (maybe event hooks) - particularly in the areas of dropped or returned messages, connection failures and reconnect attempts
4. Lightweight interface and configuration - hopefully


## Basic usage

Build connections with io.relayr.amqp.ConnectionHolder.builder

Create a connection:

```
val connection = ConnectionHolder.builder("amqps://guest:password@host:port")
  .eventHooks(EventHooks(eventListener))
  .reconnectionStrategy(ReconnectionStrategy.JavaClientFixedReconnectDelay(1 second))
  .build()
```

Create a channel:

```
val channel = connection.newChannel()
```

Create an RPC server listening on queue "queue.name", expecting a String and echoing it back:

```
def rpcHandler(request: Message): Future[Message] = request match {
  case Message.String(string) => Future(Message.String(string))
}
val queue = QueueDeclare(Some("queue.name"))
val rpcServerCloser = channel.rpcServer(queue, AckOnHandled)(rpcHandler)
```

Create an RPC client method which sends requests to the queue "queue.name" with a response timeout of 10 seconds :

```
val rpcClient = RPCClient(channel)
val rpcMethod = rpcClient.newMethod(Exchange.Default.route("queue.name"), 10 second)
```

Create a consumer on "queue.name" printing out strings sent to it:

```
def consumer(request: Message): Unit = request match {
  case Message.String(string) => println(string)
}
val queue = QueueDeclare(Some("queue.name"))
channel.addConsumer(queue, consumer)
```

Send a message to "queue.name":

```
channel.send(Exchange.Default.route("queue.name"), Message.String("message")
```

## Contribution

Pull Requests welcome as well as Github issues
