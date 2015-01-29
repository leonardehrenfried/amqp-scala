package io.relayr.amqp

import io.relayr.amqp.properties.Key.{ ContentType, ContentEncoding }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class MessageSpec extends FlatSpec with Matchers with MockFactory {

  "Messages" should "be constructed properly from json strings" in {
    val m = Message.JSONString("json")
    val ContentEncoding("UTF-8") = m.messageProperties
    val ContentType("application/json") = m.messageProperties
    val Message.JSONString(s) = m
    s should be ("json")
  }

  it should "be constructed properly from arrays" in {

  }
}
