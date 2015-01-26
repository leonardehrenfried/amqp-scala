package io.relayr.amqp

/** Wrapped immutable array of Bytes, produces a defensive copy of the passed array */
class ByteArray(array: Array[Byte]) extends Traversable[Byte] {
  private val _array = array.clone()

  //  def toArray: Array[Byte] =
  //    _array.clone()

  override def foreach[U](f: (Byte) ⇒ U): Unit =
    _array.foreach(f)

  override def equals(other: Any): Boolean = other match {
    case that: ByteArray ⇒
      _array sameElements that._array
    case _ ⇒ false
  }

  override def hashCode(): Int = {
    val state = Seq(_array)
    state.map(_.hashCode()).foldLeft(0)((a, b) ⇒ 31 * a + b)
  }
}

/** Produces an immutable array of bytes */
object ByteArray {
  def apply(array: Array[Byte]): ByteArray = new ByteArray(array)
}
