import java.util.Base64

object SortUtils {
  def compare(keyvalue1: Array[Byte], keyvalue2: Array[Byte]): Boolean = {
    def compareByte(keyvalue1: Array[Byte], keyvalue2: Array[Byte], k: Int): Boolean = {
      if (k == 10) false
      else if (keyvalue1(k) < keyvalue2(k)) true
      else if (keyvalue1(k) > keyvalue2(k)) false
      else compareByte(keyvalue1, keyvalue2, k + 1)
    }
    compareByte(keyvalue1, keyvalue2, 0)
  }
  def compareInt(keyvalue1: Array[Byte], keyvalue2: Array[Byte]): Int = {
    def compareByte(keyvalue1: Array[Byte], keyvalue2: Array[Byte], k: Int): Int = {
      if (k == 10) 0
      else if (keyvalue1(k) < keyvalue2(k)) 1
      else if (keyvalue1(k) > keyvalue2(k)) -1
      else compareByte(keyvalue1, keyvalue2, k + 1)
    }
    compareByte(keyvalue1, keyvalue2, 0)
  }
  def bytesToString(keyvalue: Array[Byte]) = {
    keyvalue.take(10).map(_.toInt).mkString(",")
  }

  def encodeBytes(keyvalue: Array[Byte]): String = {
    Base64.getEncoder.encodeToString(keyvalue)
  }

  def decodeString(str: String): Array[Byte] = {
    Base64.getDecoder.decode(str)
  }
}
