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

case class PRISMDriverMongoDB(
    blockfrostConfig: BlockfrostConfig,
    wallet: CardanoWalletConfig,
    didPrism: DIDPrism,
    vdrKey: Secp256k1PrivateKey,
    mongoDBConnection: String = "mongodb+srv://readonly:readonly@cluster0.bgnyyy1.mongodb.net/indexer"
) extends PRISMDriver {
  val prismStateLayer = AsyncDriverResource.layer >>> PrismStateMongoDB.makeReadOnlyLayer(mongoDBConnection)
  val chain = PrismChainServiceImpl(blockfrostConfig, wallet)
  def vdrServiceLayer = // TODO make layer in method in dependency
    ZLayer.fromZIO { ZIO.service[PrismStateRead].map(prismState => VDRServiceImpl(chain, prismState)) }

  override def run[E, A](program: ZIO[VDRService, E, A]): A = {
    PRISMDriver.runProgram[E, A](program.provideLayer(prismStateLayer.orDie >>> vdrServiceLayer))
  }

  override def getIdentifier: String = "PRISMDriverMongoDB"

}
