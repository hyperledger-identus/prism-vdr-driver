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
) extends PRISMReadOnlyDriver {
  val prismStateZLayer = AsyncDriverResource.layer >>> PrismStateMongoDB.makeReadOnlyLayer(mongoDBConnection)
  val chain = PrismChainServiceImpl(blockfrostConfig, wallet)
  def vdrServiceLayer = // TODO make layer in method in dependency
    ZLayer.fromZIO { ZIO.service[PrismStateRead].map(prismState => VDRPassiveServiceImp(prismState)) }

  def run[E, A](program: ZIO[VDRPassiveService, E, A]): A = {
    PRISMDriver.runProgram[E, A](program.provideLayer(prismStateZLayer.orDie >>> vdrServiceLayer))
  }

  def getIdentifier: String = "PRISMDriverMongoDB"

}

case class PRISMDriverMongoDBWithIndexer(
    blockfrostConfig: BlockfrostConfig,
    wallet: CardanoWalletConfig,
    didPrism: DIDPrism,
    vdrKey: Secp256k1PrivateKey,
    mongoDBConnection: String
) extends PRISMDriver {
  val prismStateLayer = AsyncDriverResource.layer >>> PrismStateMongoDB.makeLayer(mongoDBConnection)
  val blockfrostConfigLayer = ZLayer.succeed(blockfrostConfig)
  val chain = PrismChainServiceImpl(blockfrostConfig, wallet)
  def vdrServiceLayer = // TODO make layer in method in dependency
    ZLayer.fromZIO { ZIO.service[PrismState].map(prismState => VDRServiceImpl(chain, prismState)) }

  def run[E, A](program: ZIO[VDRService, E, A]): A = {
    PRISMDriver.runProgram[E, A](
      for {
        _ <- ZIO.log(s"Indexer start: $mongoDBConnection")
        eventCounter <- vdr.Indexer.indexerJobDB.provideLayer(prismStateLayer ++ blockfrostConfigLayer).orDie
        _ <- ZIO.log(s"Inddexed with $eventCounter Events")
        ret <- program.provideLayer(prismStateLayer.orDie >>> vdrServiceLayer)
      } yield ret
    )
  }

  def getIdentifier: String = "PRISMDriverMongoDBWithIndexer"

  def index(blockfrostConfig: BlockfrostConfig): ZIO[Any, Unit, Unit] =
    for {
      _ <- ZIO.log(s"Index latest data")
      eventCounter <- vdr.Indexer.indexerJobDB.provideLayer(prismStateLayer ++ blockfrostConfigLayer).orDie
      _ <- ZIO.log(s"Inddexed with $eventCounter Events")
    } yield ()

}
