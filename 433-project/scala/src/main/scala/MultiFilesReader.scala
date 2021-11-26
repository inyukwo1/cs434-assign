import java.io.File
import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.Buffer

object MultiFilesReader {

  case class FileReader(filePath: String)  {
    private val gensortReader = new GensortFileReader(filePath)
    private lazy val reader: Iterator[Array[Byte]] = gensortReader.stream().iterator
    private var headOption: Option[Array[Byte]] = None

    def currentHeadOption: Option[Array[Byte]] = headOption match {
      case Some(str) => Some(str)
      case None => {
        this.next()
        this.headOption
      }
    }

    def next(): Option[Array[Byte]] = if (reader.hasNext) {
      val str = reader.next
      this.headOption = Some(str)
      Some(str)
    } else {
      if (this.headOption.isDefined) this.headOption = None
      None
    }

    def close() = {
      gensortReader.close()
    }
  }
  object KeyValueOrdering extends Ordering[FileReader] {
    def compare(a:FileReader, b:FileReader): Int = {
      assert (a.currentHeadOption.nonEmpty && b.currentHeadOption.nonEmpty)
      SortUtils.compareInt(a.currentHeadOption.get,  b.currentHeadOption.get)
    }
  }

  object FileReader {
    val ord: Ordering[FileReader] = KeyValueOrdering

  }

  case class FilesReader(filePaths: String*) {
    private lazy val readers: mutable.Buffer[FileReader] = filePaths.map(FileReader(_)).toBuffer
    var lastReader: Option[Int] = if (readers.nonEmpty) Some(0) else None

    @tailrec
    final def readLine(): Option[Array[Byte]] = lastReader match {
      case Some(i) => readers(i) next() match {
        case Some(str) => Some(str)
        case None => {
          if (readers.size > 1) {
            readers remove i
            lastReader = Some(i % readers.size)
            readLine()
          } else {
            None
          }
        }
      }
      case None => None
    }
  }


}