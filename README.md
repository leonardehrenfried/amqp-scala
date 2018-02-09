[![Build Status](https://travis-ci.org/leonardehrenfried/rabbitmq-scala-client.svg?branch=master)](https://travis-ci.org/leonardehrenfried/rabbitmq-scala-client)
[![Coverage Status](https://coveralls.io/repos/relayr/rabbitmq-scala-client/badge.svg?branch=master)](https://coveralls.io/r/relayr/rabbitmq-scala-client?branch=master)

# rabbitmq-scala-client

Wrapper around the rabbitmq-java-client for better scala usage. Much of this is based on 
[amqp-client](https://github.com/sstone/amqp-client) by [sstone](https://github.com/sstone). 
The main reason for the rewrite is to not require our clients to use akka, to be easier to configure and to implement 
event hooks to enable statistics gathering.

## Updates

- 0.1.6 - New methods to add consumers which accept an Envelope (encapsulating a message with the exchange and routing key it was sent to)
- 0.1.8 - Security & reliability fix - fix non-atomic update of RPC client call counter

## Features

- Sending and receiving of AMQP messages
- Support for [Lyra](https://github.com/jhalterman/lyra) reconnection strategies in the event of connection / channel / consumer failures
- Logging of dropped or returned messages, connection failures and reconnect attempts
- An implementation of the [RPC pattern over AMQP](https://www.rabbitmq.com/tutorials/tutorial-six-java.html)


## Download and inclusion on your project

The artifact is published to Maven Central. To add it to your build, add the
following to your `build.sbt`:

```scala
libraryDependencies += "io.leonard" %% "rabbitmq-scala-client" % "$latestVersion"
```

To find the latest version please visit the
[project's page on search.maven.org](http://search.maven.org/#search|gav|1|g%3A%22io.leonard%22%20AND%20a%3A%22rabbitmq-scala-client_2.12%22).


## Basic usage

Build connections with io.leonard.amqp.ConnectionHolder.builder

Create a connection:

```scala
val connection = ConnectionHolder.builder("amqps://guest:password@host:port")
  .eventHooks(EventHooks(eventListener))
  .reconnectionStrategy(ReconnectionStrategy.JavaClientFixedReconnectDelay(1 second))
  .build()
```

Create a channel:

```scala
val channel = connection.newChannel()
```

Create an RPC server listening on queue "queue.name", expecting a String and echoing it back:

```scala
def rpcHandler(request: Message): Future[Message] = request match {
  case Message.String(string) => Future(Message.String(string))
}
val queue = QueueDeclare(Some("queue.name"))
val rpcServerCloser = channel.rpcServer(queue, AckOnHandled)(rpcHandler)
```

Create an RPC client method which sends requests to the queue "queue.name" with a response timeout of 10 seconds :

```scala
val rpcClient = RPCClient(channel)
val rpcMethod = rpcClient.newMethod(Exchange.Default.route("queue.name"), 10 second)
```

Create a consumer on "queue.name" printing out strings sent to it:

```scala
def consumer(request: Message): Unit = request match {
  case Message.String(string) => println(string)
}
val queue = QueueDeclare(Some("queue.name"))
channel.addConsumer(queue, consumer)
```

Send a message to "queue.name":

```scala
channel.send(Exchange.Default.route("queue.name"), Message.String("message")
```

## Contribution

Pull Requests welcome as well as Github issues
