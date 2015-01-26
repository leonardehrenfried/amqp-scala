![Build status](https://travis-ci.org/relayr/rabbitmq-scala-client.svg?branch=master)

# rabbitmq-scala-client

NOTE: this is at very early stages and is not a working client yet

Wrapper around the rabbitmq-java-client for better scala usage. Much of this is based on 
[amqp-client](https://github.com/sstone/amqp-client) by [sstone](https://github.com/sstone). 
The main reason for the rewrite is to not require our clients to use akka, to be easier to configure and to implement 
event hooks to enable statistics gathering.


## Requirements

1. We currently use the AMQP RPC pattern extensively, so Non-blocking RPC should be fairly easy
2. Configurable reconnection strategies - (exponential backoff for one)
3. We have changing logging and statistics requirements, so we need some solution for that (maybe event hooks) - particularly in the areas of dropped or returned messages, connection failures and reconnect attempts
4. Lightweight interface and configuration - hopefully
