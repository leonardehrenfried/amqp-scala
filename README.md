# rabbitmq-scala-client

Wrapper around the rabbitmq-java-client for better scala usage.


## Requirements

1. We currently use the AMQP RPC pattern extensively, so Non-blocking RPC should be fairly easy
2. Configurable reconnection strategies - (exponential backoff for one)
3. We have changing logging and statistics requirements, so we need some solution for that (maybe event hooks) - particularly in the areas of dropped or returned messages, connection failures and reconnect attempts
4. Lightweight interface and configuration - hopefully
