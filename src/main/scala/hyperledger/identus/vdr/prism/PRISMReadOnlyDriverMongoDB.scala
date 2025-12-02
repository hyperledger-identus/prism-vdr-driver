package hyperledger.identus.vdr.prism

import zio.*
import scala.jdk.CollectionConverters.*
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.*
import fmgp.did.method.prism.vdr.*
import interfaces.Driver
import interfaces.Proof
import interfaces.Driver.OperationState
import fmgp.crypto.Secp256k1PrivateKey

import zio.json._
import zio.stream.ZStream
import zio.stream.ZPipeline
import fmgp.did.method.prism.mongo.AsyncDriverResource

case class PRISMReadOnlyDriverMongoDB(
    blockfrostConfig: BlockfrostConfig,
    wallet: CardanoWalletConfig,
    didPrism: DIDPrism,
    vdrKey: Secp256k1PrivateKey,
    mongoDBConnection: String // = "mongodb+srv://readonly:readonly@cluster0.bgnyyy1.mongodb.net/indexer"
) extends PRISMReadOnlyDriver {
  val prismStateZLayer = AsyncDriverResource.layer >>> PrismStateMongoDB.makeReadOnlyLayer(mongoDBConnection)
  val chain = PrismChainServiceImpl(blockfrostConfig, wallet)
  def vdrServiceLayer = // TODO make layer in method in dependency
    ZLayer.fromZIO { ZIO.service[PrismStateRead].map(prismState => VDRPassiveServiceImpl(prismState)) }

  override protected def runWithVDRPassiveService[E, A](program: ZIO[VDRPassiveService, E, A]): A = {
    PRISMDriver.runProgram[E, A](program.provideLayer(prismStateZLayer.orDie >>> vdrServiceLayer))
  }

  def getIdentifier: String = "PRISMDriverMongoDB"

  override def create(
      data: Array[Byte],
      options: java.util.Map[String, ?]
  ): interfaces.Driver.OperationResult = ??? // This is intentional

  override def update(
      data: Array[Byte],
      paths: Array[String],
      queries: java.util.Map[String, String],
      fragment: String,
      options: java.util.Map[String, ?]
  ): interfaces.Driver.OperationResult = ??? // This is intentional

  override def delete(
      paths: Array[String],
      queries: java.util.Map[String, String],
      fragment: String,
      options: java.util.Map[String, ?]
  ): Unit = ??? // This is intentional

}
