package io.relayr.amqp.util

import java.io.File

import com.google.common.io.Files
import org.apache.qpid.server.{Broker, BrokerOptions}
import org.slf4j.LoggerFactory

trait EmbeddedAMQPBroker {
   // keystore.jks password = "CHANGEME"
   private val log = LoggerFactory.getLogger("EmbeddedAMQPBroker")

   private def tmpFolder = Files.createTempDir()
   private var broker: Broker = null

   def brokerAmqpPort = 9569
   def brokerHttpPort = 9568

   private def qpidHomeDir = "src/test/resources/"
   private def configFileName = "/test-config.json"

   def initializeBroker(): Unit = {
     broker = new Broker()
     val brokerOptions = new BrokerOptions()

     val file = new File(qpidHomeDir)
     val homePath = file.getAbsolutePath
     log.info(" qpid home dir=" + homePath)
     log.info(" qpid work dir=" + tmpFolder.getAbsolutePath)

     brokerOptions.setConfigProperty("qpid.work_dir", tmpFolder.getAbsolutePath)

     brokerOptions.setConfigProperty("qpid.amqp_port",s"$brokerAmqpPort")
     brokerOptions.setConfigProperty("qpid.http_port", s"$brokerHttpPort")
     brokerOptions.setConfigProperty("qpid.home_dir", homePath)

     brokerOptions.setInitialConfigurationLocation(homePath + configFileName)
     broker.startup(brokerOptions)
     log.info("broker started")
   }

   def shutdownBroker() {
     broker.shutdown()
   }
 }
