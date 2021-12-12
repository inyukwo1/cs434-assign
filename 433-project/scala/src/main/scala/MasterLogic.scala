import java.util.logging.Logger
import scala.collection.mutable.ListBuffer

class MasterLogic(masterLogicToGRPCServer: MasterLogicToGRPCServer) {
  private[this] val logger = Logger.getLogger(classOf[MasterLogic].getName)
  def logic(): Unit = {
    val (ips: ListBuffer[String], ids: ListBuffer[String]) = handShake()
    println(ips.mkString(", "))
    val samplesKeys: List[Array[Byte]] = receiveSampledKeys()

    def toDistinct(head: Array[Byte], sampledKeys: List[Array[Byte]]): List[Array[Byte]] = {
      if (sampledKeys.isEmpty) head :: List()
      else if (SortUtils.compareInt(head, sampledKeys.head) == 0) toDistinct(head, sampledKeys.tail)
      else head :: toDistinct(sampledKeys.head, sampledKeys.tail)
    }
    val sorted = samplesKeys.sortWith(SortUtils.compare)
    val sortedSampledKeys = toDistinct(sorted.head, sorted.tail)
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
