import org.scalatest.flatspec.AnyFlatSpec

import scala.sys.process._

class ExternalSortTest extends AnyFlatSpec {

  "Sorted file" should "sorted" in {
    val origDataPath = "../data/slave1/input1/input"
    val sortedPath = "../data/tmp"
    new ExternalSorting(origDataPath, sortedPath, 100000).sort()

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
