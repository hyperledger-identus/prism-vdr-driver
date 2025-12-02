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

object PRISMDriverInMemory {

  def loadPrismStateFromChunkFiles: ZIO[IndexerConfig & PrismState, Throwable, PrismState] = for {
    indexerConfig <- ZIO.service[IndexerConfig]
    chunkFilesAfter <- fmgp.did.method.prism.vdr.Indexer
      .findChunkFiles(rawMetadataPath = indexerConfig.rawMetadataPath)
    _ <- ZIO.log(s"Read chunkFiles (${chunkFilesAfter.length})")
    streamAllMaybeEventFromChunkFiles = ZStream.fromIterable {
      chunkFilesAfter.map { fileName =>
        ZStream
          .fromFile(fileName)
          .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
          .map { _.fromJson[CardanoMetadataCBOR].getOrElse(???) }
          .via(IndexerUtils.pipelineTransformCardanoMetadata2SeqEvents)
          .flatMap(e => ZStream.fromIterable(e))
      }
    }.flatten
    _ <- ZIO.log(s"Init PrismState")
    state <- ZIO.service[PrismState]
    countEvents <- streamAllMaybeEventFromChunkFiles
      .via(IndexerUtils.pipelinePrismState)
      .run(IndexerUtils.countEvents) // (ZSink.count)
      .provideEnvironment(ZEnvironment(state: PrismState))
    _ <- ZIO.log(s"Finish Init PrismState: $countEvents")
    ssiCount <- state.ssiCount
    vdrCount <- state.vdrCount
    _ <- ZIO.log(s"PrismState was $ssiCount SSI and $vdrCount VDR")
  } yield state

  def apply(
      blockfrostConfig: BlockfrostConfig,
      wallet: CardanoWalletConfig,
      didPrism: DIDPrism,
      vdrKey: Secp256k1PrivateKey,
      // keyName: String = "vdr1",
      workdir: String = "../../prism-vdr/mainnet"
  ): PRISMDriverInMemory = {
    val chain: PrismChainService = PrismChainServiceImpl(blockfrostConfig, wallet)
    val prismState = PRISMDriver.runProgram(
      for {
        prismState <- PrismStateInMemory.empty
        indexerConfig: IndexerConfig = IndexerConfig(mBlockfrostConfig = Some(blockfrostConfig), workdir = workdir)
        prismStateLayer = ZLayer.succeed(prismState)
        indexerConfigLayer = ZLayer.succeed(indexerConfig)
        _ <- /*IndexerUtils.*/ loadPrismStateFromChunkFiles.provide(prismStateLayer ++ indexerConfigLayer)
      } yield (prismState)
    )

    val vdrService: VDRService = VDRServiceImpl(chain, prismState)
    new PRISMDriverInMemory(vdrService, didPrism, vdrKey)
  }
}

case class PRISMDriverInMemory(
    vdrService: VDRService,
    didPrism: DIDPrism,
    vdrKey: Secp256k1PrivateKey,
) extends PRISMDriver {

  override def run[E, A](program: ZIO[VDRService, E, A]): A =
    PRISMDriver.runProgram[E, A](program.provideEnvironment(ZEnvironment(vdrService)))

  def getIdentifier: String = "PRISMDriverInMemory"

}
