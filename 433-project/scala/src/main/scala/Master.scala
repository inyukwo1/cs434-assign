import com.example.protos.main.{MainGrpc, HandShakeReply, HandShakeRequest}
import io.grpc.{Server, ServerBuilder}
import java.net._
import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, Future}

/**
 * [[https://github.com/grpc/grpc-java/blob/v0.15.0/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java]]
 */
object Master {
  private val logger = Logger.getLogger(classOf[Master].getName)

  def main_(args: Array[String]): Unit = {
    val server = new Master(ExecutionContext.global, args(1).toInt)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class Master(executionContext: ExecutionContext, numSlaves: Int) { self =>
  private[this] var server: Server = null

  def start(): Unit = {
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
    val masterLogicToGRPCServer: MasterLogicToGRPCServer = new MasterLogicToGRPCServer(numSlaves)

    server = ServerBuilder.forPort(Master.port).addService(MainGrpc.bindService(new MasterServerImpl(masterLogicToGRPCServer), executionContext)).maxInboundMessageSize(100000000).build.start
    val localhost: InetAddress = InetAddress.getLocalHost
    val localIpAddress: String = localhost.getHostAddress
    Master.logger.info("Server started, listening on " + localIpAddress + ":" + Master.port)
    val masterLogic = new MasterLogic(masterLogicToGRPCServer)
    masterLogic.logic()
    stop()
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }
}
