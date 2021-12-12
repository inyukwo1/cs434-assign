import java.util.logging.Logger
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

class MasterLogic(masterLogicToGRPCServer: MasterLogicToGRPCServer) {
  private[this] val logger = Logger.getLogger(classOf[MasterLogic].getName)
  def logic(): Unit = {
    val (ips: ListBuffer[String], ids: ListBuffer[String]) = handShake()
    println(ips.mkString(", "))
    val samplesKeys: List[Array[Byte]] = receiveSampledKeys()

    @tailrec
    def toDistinct(output: List[Array[Byte]], remainingInput: List[Array[Byte]]): List[Array[Byte]] = {
      if (remainingInput.isEmpty) output
      else if (SortUtils.compareInt(output.last, remainingInput.head) == 0) toDistinct(output, remainingInput.tail)
      else toDistinct(output :+ remainingInput.head, remainingInput.tail)
    }
    val sorted = samplesKeys.sortWith(SortUtils.compare)
    val sortedSampledKeys = toDistinct(List(sorted.head), sorted.tail)
    logger.info("Sorting sampled keys")
    shareSampledKeys(sortedSampledKeys)
    val sampledKeysLength = sortedSampledKeys.length
    logger.info("Sampled keys length: " + sampledKeysLength)
    List.range(0, sampledKeysLength).foreach((index: Int)=> {
      logger.info("Receive / Distributing: " + index + ", " + SortUtils.bytesToString(sortedSampledKeys(index)))
      val receivedData: List[Array[Byte]] = requestDataOnce()
      logger.info("Received: " + receivedData.length + ", " + masterLogicToGRPCServer.responseCount)
      val sortedReceivedData = receivedData.sortWith(SortUtils.compare)
      val targetSlaveIndex = ids.length * index / sampledKeysLength
      val targetId = ids(targetSlaveIndex)
      sendData(index, sortedReceivedData, targetId)
    })
    val receivedData: List[Array[Byte]] = requestDataOnce()
    val sortedReceivedData = receivedData.sortWith(SortUtils.compare)
    sendData(sampledKeysLength, sortedReceivedData, ids.last)
    logger.info("Done")
  }

  def handShake(): (ListBuffer[String], ListBuffer[String]) = {
    masterLogicToGRPCServer.reset()
    logger.info("Wait handShaking")
    masterLogicToGRPCServer.waitPrevDone()
    logger.info("HandShaking done")
    (masterLogicToGRPCServer.ips, masterLogicToGRPCServer.ids)
  }

  def receiveSampledKeys(): List[Array[Byte]] = {
    logger.info("Wait receiving sampled Keys")
    masterLogicToGRPCServer.reset()
    masterLogicToGRPCServer.waitPrevDone()
    logger.info("Received sampled Keys")
    masterLogicToGRPCServer.sampledKeys.toList
  }

  def shareSampledKeys(list: List[Array[Byte]]) = {
    masterLogicToGRPCServer.sortedSampledKeys = list
    masterLogicToGRPCServer.reset()
    masterLogicToGRPCServer.waitPrevDone()
  }

  def requestDataOnce(): List[Array[Byte]] = {
    masterLogicToGRPCServer.reset()
    masterLogicToGRPCServer.waitPrevDone()
    masterLogicToGRPCServer.receivedData.toList
  }

  def sendData(i: Int, list: List[Array[Byte]], str: String) = {
    masterLogicToGRPCServer.receivedData = new ListBuffer[Array[Byte]]()
    masterLogicToGRPCServer.sendData = list
    masterLogicToGRPCServer.sendDataIdx = i
    masterLogicToGRPCServer.sendDataId = str
    masterLogicToGRPCServer.reset()
    masterLogicToGRPCServer.waitPrevDone()
  }

}
