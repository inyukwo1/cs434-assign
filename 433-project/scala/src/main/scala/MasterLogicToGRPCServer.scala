import scala.collection.mutable.ListBuffer

class MasterLogicToGRPCServer(val slaveNum: Int) {
  var responseCount = 0
  var responseDoneCount = 0
  var ressetted = false
  // For handshake
  var ips = new ListBuffer[String]()
  var ids = new ListBuffer[String]()
  // For receiveSampledKeys
  var sampledKeys = new ListBuffer[Array[Byte]]()
  // For shareSampledKeys
  var sortedSampledKeys = List.empty[Array[Byte]]
  // For requestDataOnce
  var receivedData = new ListBuffer[Array[Byte]]()
  // For sendData
  var sendData = List.empty[Array[Byte]]
  var sendDataIdx = 0
  var sendDataId = ""

  def responseHandShake(ip: String, id: String): Unit = {
    this.synchronized {
      ips += ip
      ids += id
    }
    response()
  }

  def responseReceiveSampledKeys(data: List[Array[Byte]]) = {
    this.synchronized {
      data.foreach((f: Array[Byte])=> {
        sampledKeys += f
      })
    }
    response()
  }

  def responseRequestDataOnce(data: List[Array[Byte]]) = {
    this.synchronized {
      data.foreach((f: Array[Byte])=> {
        receivedData += f
      })
    }
    println("Receiving: " + receivedData.length)
    response()
  }

  def reset(): Unit = {
    this.synchronized{
      responseCount = 0
      responseDoneCount = 0
      ressetted = true
    }
  }

  def response(): Unit = {
    this.synchronized{
      responseCount += 1
    }
  }

  def waitResponse(): Unit = {
    while (true) {
      this.synchronized{
        if (responseCount == slaveNum) {
          ressetted = false
          responseDoneCount += 1
          return
        }
      }
      Thread.sleep(10)
    }
  }

  def waitPrevDone(): Unit = {
    while (true) {
      this.synchronized{
        if (responseDoneCount == slaveNum) {
          responseDoneCount = 0
          return
        }
      }
      Thread.sleep(10)
    }
  }

  def waitReset(): Unit = {
    while (true) {
      this.synchronized{
        if (ressetted) return
      }
      Thread.sleep(1)
    }
  }
}
