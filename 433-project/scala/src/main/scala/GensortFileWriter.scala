import java.io._

class GensortFileWriter(var path: String) {
  val target = new BufferedOutputStream(new FileOutputStream(path))
  def write(bytes: Array[Byte]): Unit = {
    target.write(bytes)
  }

  def close(): Unit = {
    target.close()
  }
}
