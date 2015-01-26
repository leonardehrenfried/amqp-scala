package io.relayr.amqp

/** Wrapped immutable array of Bytes, produces a defensive copy of the passed array */
class ByteArray(array: Array[Byte]) extends Traversable[Byte] {
  private val _array = array.clone()

  //  def toArray: Array[Byte] =
  //    _array.clone()

  override def foreach[U](f: (Byte) ⇒ U): Unit =
    _array.foreach(f)
}

/** Produces an immutable array of bytes */
object ByteArray {
  def apply(array: Array[Byte]): ByteArray = new ByteArray(array)
}
