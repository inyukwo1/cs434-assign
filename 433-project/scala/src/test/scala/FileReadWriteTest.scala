import org.scalatest.flatspec.AnyFlatSpec

import java.io.{BufferedInputStream, BufferedOutputStream, FileInputStream, FileOutputStream}


class FileReadWriteTest extends AnyFlatSpec {

  def assertSame(file1path: String, file2path: String): Unit = {
    val file1Stream = new BufferedInputStream( new FileInputStream(file1path) )
    val file2Stream = new BufferedInputStream( new FileInputStream(file2path) )
    // because this takes too long
    for (i <- 1 until 100000) {
      assert (file1Stream.read() == file2Stream.read())
      assert (file1Stream.available() == file2Stream.available())
    }
  }

  "Copied file" should "same with original file" in {
    val origDataPath = "../data/slave1/input1/input"
    val copyPath = "../data/tmp"
    val filereader = new GensortFileReader(origDataPath)
    val filewriter = new GensortFileWriter(copyPath)
    filereader.stream().foreach((bytes: Array[Byte]) => filewriter.write(bytes))
    filereader.close()
    filewriter.close()
    assertSame(origDataPath, copyPath)
  }
}
