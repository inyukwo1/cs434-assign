import com.example.protos.main.{HandShakeRequest, MainGrpc, ReceiveSampledKeysRequest, RequestDataOnceRequest, SendDataRequest, ShareSampledKeysRequest}
import com.example.protos.main.MainGrpc.MainBlockingStub
import java.net._
import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}

import java.io.File
import scala.collection.mutable.ListBuffer
import scala.collection.parallel.mutable.ParArray

object Slave {
  def apply(host: String, port: Int, inputDirs: Array[String], outputDir: String): Slave = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val blockingStub = MainGrpc.blockingStub(channel)
    new Slave(channel, blockingStub, inputDirs, outputDir)
  }

  def main(args: Array[String]): Unit = {
    println(args.mkString(" "))
    val idxcolon = args(1).indexOf(":")
    val client = Slave(args(1).substring(0, idxcolon), args(1).substring(idxcolon + 1).toInt, args.slice(2, args.length - 2), args(args.length - 1))
    try {
      val numSlaves = client.handShake()
      val sortedFilePaths = client.externalSort()
      println("External sort done")
      client.sendSampledKeys(sortedFilePaths, numSlaves)
      val sortedKeys: List[Array[Byte]] = client.receiveSortedSampledKeys()
      val readers = sortedFilePaths.map(new GensortFileReader(_))
      var iterator_and_head = readers.map((f: GensortFileReader)=> {
        val iterator = f.stream().iterator
        (iterator, Option(iterator.next()))
      })
      sortedKeys.foreach((f: Array[Byte])=> {
        println("sending / writing: " + SortUtils.bytesToString(f))
        iterator_and_head = client.sendDataOnce(iterator_and_head, f)
        client.receiveAndWriteData()
      })
      readers.foreach(_.close())
      sortedFilePaths.foreach(new File(_).delete())
    } finally {
      client.shutdown()
    }
  }
}

class Slave private(
                     private val channel: ManagedChannel,
                     private val blockingStub: MainBlockingStub,
                     private val inputDirs: Array[String],
                     private val outputDir: String
                   ) {
  private[this] val logger = Logger.getLogger(classOf[Slave].getName)
  val id: String = java.util.UUID.randomUUID.toString

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def handShake(): Int = {
    val localhost: InetAddress = InetAddress.getLocalHost
    val localIpAddress: String = localhost.getHostAddress

    logger.info("Handshaking, I'm " + localIpAddress + ", " + id)
    val request = HandShakeRequest(ip = localIpAddress, id = id)
    try {
      val response = blockingStub.handShake(request)
      logger.info("Handshake done")
      response.numSlaves
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
        1
    }
  }

  def getListOfFiles(dir: String): List[String] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).map(_.toString).toList
    } else {
      List[String]()
    }
  }

  def externalSort(): ParArray[String] = {
    val fileList = inputDirs.flatMap(getListOfFiles)
    fileList.zipWithIndex.par.map {case (f: String, i: Int) => {
      val sortedFilePath = outputDir + "/" + i.toString + "_sorted"
      new ExternalSorting(f, sortedFilePath, 100000).sort()
      sortedFilePath
    }}
  }

  def sendSampledKeys(sortedFilePaths: ParArray[String], numSlaves: Int): Unit = {
    val sampledKeys: List[Array[Byte]] = sortedFilePaths.flatMap((f: String)=> {
      val fileReader = new GensortFileReader(f)
      val iterator = fileReader.stream().iterator
      val sampledKeys = new ListBuffer[Array[Byte]]()

      iterator.drop(10000 / numSlaves)
      while (iterator.nonEmpty) {
        sampledKeys.synchronized {
          sampledKeys += iterator.next()
        }
        iterator.drop(10000 / numSlaves)
      }
      fileReader.close()
      sampledKeys
    }).toList
    val request = ReceiveSampledKeysRequest(sampledKeys = sampledKeys.map(SortUtils.encodeBytes))
    try {
      blockingStub.receiveSampledKeys(request)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }

  def receiveSortedSampledKeys(): List[Array[Byte]] = {
    val request = ShareSampledKeysRequest()
    try {
      val response = blockingStub.shareSampledKeys(request)
      response.sampledKeys.map(SortUtils.decodeString).toList
    }
    catch {
      case e: StatusRuntimeException => {
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
        assert(false)
        List.empty[Array[Byte]]
      }
    }
  }

  def sendDataOnce(iterator_and_head: ParArray[(Iterator[Array[Byte]], Option[Array[Byte]])], key: Array[Byte]): ParArray[(Iterator[Array[Byte]], Option[Array[Byte]])] = {
    val data = new ListBuffer[Array[Byte]]()
    val new_iteraor_and_head: ParArray[(Iterator[Array[Byte]], Option[Array[Byte]])] = iterator_and_head.map((f: (Iterator[Array[Byte]], Option[Array[Byte]]))=> {
      var (it, head) = f
      while (head.nonEmpty && SortUtils.compare(head.get, key)) {
        data.synchronized {
          data += head.get
        }
        if (it.hasNext) head = Option(it.next())
        else head = None
      }
      (it, head)
    })
    val request = RequestDataOnceRequest(data = data.map(SortUtils.encodeBytes))
    try {
      val response = blockingStub.requestDataOnce(request)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    new_iteraor_and_head
  }

  def receiveAndWriteData() = {
    val request = SendDataRequest(id = id)
    try {
      val response = blockingStub.sendData(request)
      if (response.data.nonEmpty) {
        val writer = new GensortFileWriter(outputDir + "/partition." + response.index)
        response.data.foreach((f: String) => writer.write(SortUtils.decodeString(f)))
        writer.close()
      }
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }
}