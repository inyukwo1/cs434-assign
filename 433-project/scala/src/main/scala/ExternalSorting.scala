// reference: https://github.com/bepcyc/external-sorting/blob/master/src/main/scala/org/viacheslav/rodionov/externalsorting/ExternalSorting.scala

import MultiFilesReader.FileReader

import java.io.File
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

case class ExternalSorting(inputFile: String, outputFile: String, capacity: Int) {
  lazy private val keyvalues: Iterator[Array[Byte]] = new GensortFileReader(inputFile).stream().iterator

  def readIntoMemory: Iterator[Array[Byte]] = keyvalues take capacity

  def writeNextSortedBlock: Option[String] = {
    readIntoMemory match {
      case it if it.isEmpty => None
      case nonEmptyIterator => {
        val block: mutable.Buffer[Array[Byte]] = nonEmptyIterator.toBuffer
        ExternalSorting.saveToFile(block.sortWith(SortUtils.compare))
      }
    }
  }

  def sort(): Unit = {
    val intermediateFiles =
      Stream from 1 map (x => writeNextSortedBlock) takeWhile (_.isDefined) map (_.get)
    println(intermediateFiles.toList.length)
    ExternalSorting.merge(outputFile, intermediateFiles: _*)
  }
}

object ExternalSorting {

  def saveToFile(sortedLines: mutable.Buffer[Array[Byte]]): Option[String] = {
    Try {
      val tempFilePath = "tmp" + java.util.UUID.randomUUID.toString
      val writer = new GensortFileWriter(tempFilePath)
      sortedLines.foreach(line=>writer.write(line))
      writer.close()
      tempFilePath
    } match {
      case Success(outFile) => Some(outFile)
      case Failure(exception) => {
        exception.printStackTrace()
        None
      }
    }
  }

  def merge(outFile: String, files: String*): Unit = {
    implicit val ordering: Ordering[FileReader] = FileReader.ord
    val heap: mutable.PriorityQueue[FileReader] = mutable.PriorityQueue[FileReader]()
    val readers = files map (f => new FileReader(f))
    heap enqueue (readers: _*)
    val writer = new GensortFileWriter(outFile)

    while (heap.nonEmpty) {
      val minimal: FileReader = heap dequeue()
      minimal.currentHeadOption match {
        case Some(str) => {
          writer.write(str)
          minimal next() match {
            case None => {
              // we're done with this reader
              minimal.close()
              new File(minimal.filePath).delete()
            }
            case Some(nextStr) => heap enqueue minimal
          }
        }
        case None => {
          // we're done with this reader
          assert (false)
        }
      }
    }
    writer.close()
  }

}