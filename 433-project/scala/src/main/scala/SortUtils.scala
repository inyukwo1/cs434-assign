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
}
