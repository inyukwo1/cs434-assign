import scala.io.{BufferedSource, Source}
import java.io._
import scala.annotation.tailrec

class GensortFileReader(var path: String) {
  val source = new BufferedInputStream( new FileInputStream(path))
  def stream(): Stream[Array[Byte]] = {
    def toKeyValueStream(source: BufferedInputStream): Stream[Array[Byte]] = {
      if (source.available() == 0) Stream.Empty
      else {
        val head = Array.ofDim[Byte](100)
        source.read(head, 0, 100)
        Stream.cons(head, toKeyValueStream(source))
      }
    }
    toKeyValueStream(source)
  }

  def close(): Unit = {
    source.close()
  }
}
