name := "rabbitmq-scala-client"

version := "0.1-SNAPSHOT"

organization := "relayr"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.apache.qpid" % "qpid-broker" % "0.30" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.2.1" % "test")

libraryDependencies ++= Seq(
  "com.rabbitmq" % "amqp-client" % "3.4.2")
