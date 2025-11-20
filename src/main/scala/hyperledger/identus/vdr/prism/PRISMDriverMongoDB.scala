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
    bfConfig: BlockfrostConfig,
    wallet: CardanoWalletConfig,
    didPrism: DIDPrism,
    vdrKey: Secp256k1PrivateKey,
    mongoDBConnection: String = "mongodb+srv://readonly:readonly@cluster0.bgnyyy1.mongodb.net/indexer"
) extends PRISMDriver {
  val prismStateZLayer = AsyncDriverResource.layer >>> PrismStateMongoDB.makeLayer(mongoDBConnection)
  val chain = PrismChainServiceImpl(bfConfig, wallet)
  def vdrServiceLayer = // TODO make layer in method in dependency
    ZLayer.fromZIO { ZIO.service[PrismState].map(prismState => VDRServiceImpl(chain, prismState)) }

  def run[E, A](program: ZIO[VDRService, E, A]): A = {
    PRISMDriver.runProgram[E, A](program.provideLayer(prismStateZLayer.orDie >>> vdrServiceLayer))
  }

  def getIdentifier: String = "PRISMDriverMongoDB"

}
