import amqptest.AMQPIntegrationFixtures
import io.relayr.amqp.Event.ChannelEvent
import io.relayr.amqp._
import net.jodah.lyra.config.{RecoveryPolicy, Config}
import net.jodah.lyra.util.Duration
import org.scalatest.{FlatSpec, Matchers}

import scala.language.postfixOps

class ReconnectionIntegrationSpec extends FlatSpec with Matchers with AMQPIntegrationFixtures {
  
  val testMessage: Message = Message.String("test")

  "Lyra" should "recover from channel failures" in new ClientTestContext with ServerTestContext {
    override def clientReconnectionStrategy = ReconnectionStrategy.LyraRecoveryStrategy(
      new Config()
        .withRecoveryPolicy(new RecoveryPolicy()
          .withInterval(Duration.milliseconds(200))))

    // create client connection and bind to routing key
    clientEventListener expects ChannelEvent.ChannelOpened(1, None)
    val senderChannel: ChannelOwner = clientConnection.newChannel()

    println("Cause disconnect")
    // send to non-existent exchange cloases connection
    clientEventListener expects ChannelEvent.ChannelShutdown
    senderChannel.sendPublish(Publish(Exchange("blah").route("thing"), testMessage))

    val receiver = mockFunction[Message, Unit]
    val (serverCloser, exchange) = {
      serverEventListener expects ChannelEvent.ChannelOpened(1, None)

      val serverChannel: ChannelOwner = serverConnection.newChannel()
      val queue: QueuePassive = QueuePassive(serverChannel.declareQueue(QueueDeclare(Some("test.queue"))))
      val exchange = serverChannel.declareExchange("test-exchange", ExchangeType.topic, false, false)
      serverChannel.queueBind(queue, exchange, "test.key")
      (serverChannel.addConsumer(queue, receiver), exchange)
    }

    val destinationDescriptor = exchange.route("test.key", DeliveryMode.NotPersistent, true, true)

    Thread.sleep(1000)

    receiver expects * onCall { message: Message ⇒
      ()
    }
    println("another message")
    // send message
    senderChannel.sendPublish(Publish(destinationDescriptor, testMessage))
    Thread.sleep(1000)
  }
}
