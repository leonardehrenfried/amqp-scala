package io.relayr.amqp

import org.scalatest.{ FlatSpec, Matchers }

class ByteArraySpec extends FlatSpec with Matchers {

  "ByteArray" should "produce an array of what was put in" in {
    val i = ByteArray(Array(1: Byte))
    i.toArray should be (Array(1: Byte))
  }

  it should "not be mutatable by the producer" in {
    val m = Array(1: Byte)
    val i = ByteArray(m)
    m.update(0, 2)
    m should be (Array(2: Byte))
    i.toArray should be (Array(1: Byte))
  }

  it should "not be mutatable by the consumer" in {
    val i = ByteArray(Array(1: Byte))
    val m: Array[Byte] = i.toArray
    m.update(0, 2)
    i.toArray should be (Array(1: Byte))
  }

  it should "produce a list" in {
    val i = ByteArray(Array(1: Byte))
    i.toList should be (List(1: Byte))
  }

  it should "be mappable" in {
    val i = ByteArray(Array(1: Byte))
    i.map(1 +) should be (Seq(2: Byte))
  }
}
