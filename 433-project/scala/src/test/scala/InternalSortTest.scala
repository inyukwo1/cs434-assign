import org.scalatest.flatspec.AnyFlatSpec

import java.io.{BufferedInputStream, FileInputStream}
import scala.sys.process._
import java.nio.file.Paths

class InternalSortTest extends AnyFlatSpec {

  "Sorted file" should "sorted" in {
    val origDataPath = "../data/slave1/input1/input"
    val sortedPath = "../data/tmp"
    val filereader = new GensortFileReader(origDataPath)
    val fileWriter = new GensortFileWriter(sortedPath)
    val sorted = filereader.stream().toList.sortWith(SortUtils.compare)
    sorted.foreach(b => fileWriter.write(b))
    filereader.close()
    fileWriter.close()

    val asserter = ProcessLogger(
      (o: String) => {
        println(o)
        assert(!o.contains("ERROR"))
      },
      (e: String) => {
        println(e)
        assert(!e.contains("ERROR"))
      })

    "./valsort.sh /data/tmp" !! asserter
  }
}
