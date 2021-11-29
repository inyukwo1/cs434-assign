import scala.concurrent.Future
import com.example.protos.main.{HandShakeReply, HandShakeRequest, MainGrpc, ReceiveSampledKeysReply, ReceiveSampledKeysRequest, RequestDataOnceReply, RequestDataOnceRequest, SendDataReply, SendDataRequest, ShareSampledKeysReply, ShareSampledKeysRequest}

import java.util.Base64
import java.util.logging.Logger
import scala.collection.mutable.ListBuffer


class MasterServerImpl(masterLogicToGRPCServer: MasterLogicToGRPCServer) extends MainGrpc.Main {
  private[this] val logger = Logger.getLogger(classOf[MasterServerImpl].getName)
  override def handShake(request: HandShakeRequest): Future[HandShakeReply] = {
    masterLogicToGRPCServer.waitReset()
    val reply = HandShakeReply(numSlaves = masterLogicToGRPCServer.slaveNum)
    masterLogicToGRPCServer.responseHandShake(request.ip, request.id)
    masterLogicToGRPCServer.waitResponse()
    Future.successful(reply)
  }

  override def receiveSampledKeys(request: ReceiveSampledKeysRequest): Future[ReceiveSampledKeysReply] = {
    masterLogicToGRPCServer.waitReset()
    val reply = ReceiveSampledKeysReply()
    masterLogicToGRPCServer.responseReceiveSampledKeys(request.sampledKeys.map(SortUtils.decodeString).toList)
    masterLogicToGRPCServer.waitResponse()
    Future.successful(reply)
  }

  override def shareSampledKeys(request: ShareSampledKeysRequest): Future[ShareSampledKeysReply] = {
    masterLogicToGRPCServer.waitReset()
    val sampledKeys = masterLogicToGRPCServer.sortedSampledKeys.map(SortUtils.encodeBytes)
    val reply = ShareSampledKeysReply(sampledKeys = sampledKeys)
    masterLogicToGRPCServer.response()
    masterLogicToGRPCServer.waitResponse()
    Future.successful(reply)
  }

  override def requestDataOnce(request: RequestDataOnceRequest): Future[RequestDataOnceReply] = {
    masterLogicToGRPCServer.waitReset()
    val reply = RequestDataOnceReply()

    masterLogicToGRPCServer.responseRequestDataOnce(request.data.map(SortUtils.decodeString).toList)
    masterLogicToGRPCServer.waitResponse()
    Future.successful(reply)
  }

  override def sendData(request: SendDataRequest): Future[SendDataReply] = {
    masterLogicToGRPCServer.waitReset()
    if (request.id == masterLogicToGRPCServer.sendDataId) {
      val reply = SendDataReply(index = masterLogicToGRPCServer.sendDataIdx, data = masterLogicToGRPCServer.sendData.map(SortUtils.encodeBytes))
      masterLogicToGRPCServer.response()
      masterLogicToGRPCServer.waitResponse()
      Future.successful(reply)
    } else {
      val reply = SendDataReply(index = masterLogicToGRPCServer.sendDataIdx, data = List.empty[String])
      masterLogicToGRPCServer.response()
      masterLogicToGRPCServer.waitResponse()
      Future.successful(reply)
    }
  }
}
