package io.relayr.amqp

/** Wrapped immutable array of Bytes, produces defensive copies of the internal array */
class ByteArray(array: Array[Byte]) {
  private val _array = array.clone()

  def toArray: Array[Byte] = _array.clone()
}

/** Produces an immutable array of bytes, which is defensively copied on the way in and out */
object ByteArray {
  def apply(array: Array[Byte]): ByteArray = new ByteArray(array)
}
