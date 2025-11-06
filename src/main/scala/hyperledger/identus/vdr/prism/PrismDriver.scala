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

object PRISMDriver {

  def runProgram[E, A](program: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(program).getOrThrowFiberFailure()
    }

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
      bfConfig: BlockfrostConfig,
      wallet: CardanoWalletConfig,
      didPrism: DIDPrism,
      vdrKey: Secp256k1PrivateKey,
      // keyName: String = "vdr1",
      workdir: String = "../../prism-vdr/mainnet"
  ): PRISMDriver = {
    val chain: PrismChainService = PrismChainServiceImpl(bfConfig, wallet)
    val prismState = runProgram(
      for {
        prismState <- PrismStateInMemory.empty
        indexerConfig: IndexerConfig = IndexerConfig(mBlockfrostConfig = Some(bfConfig), workdir = workdir)
        prismStateLayer = ZLayer.succeed(prismState)
        indexerConfigLayer = ZLayer.succeed(indexerConfig)
        _ <- /*IndexerUtils.*/ loadPrismStateFromChunkFiles.provide(prismStateLayer ++ indexerConfigLayer)
      } yield (prismState)
    )

    val vdrService: VDRService = VDRServiceImpl(chain, prismState)
    new PRISMDriver(vdrService, didPrism, vdrKey)
  }
}

case class PRISMDriver(
    vdrService: VDRService,
    didPrism: DIDPrism,
    vdrKey: Secp256k1PrivateKey,
) extends Driver {

  // implement methods here (or use stub for now)
  def getFamily: String = "PRISM"
  def getIdentifier: String = "PRISMDriver"
  def getVersion: String = "1.0"
  def getSupportedVersions: Array[String] = Array("1.0")

  override def create(
      data: Array[Byte],
      options: java.util.Map[String, ?]
  ): interfaces.Driver.OperationResult = {
    PRISMDriver.runProgram(
      for {
        ret <- vdrService.createBytes(didPrism, vdrKey, data)
        (refVDR, signedPrismEvent, txHash) = ret
        out = Driver.OperationResult(
          refVDR.hex,
          Driver.OperationState.SUCCESS,
          Array(refVDR.hex),
          Map(("h", refVDR.hex)).asJava,
          null,
          null,
          null
        )
      } yield out
    )
  }

  override def update(
      data: Array[Byte],
      paths: Array[String],
      queries: java.util.Map[String, String],
      fragment: String,
      options: java.util.Map[String, ?]
  ): interfaces.Driver.OperationResult = {
    paths.headOption match
      case None =>
        throw DataCouldNotBeFoundException(
          Some("the identifier is missing from the path")
        ) // interfaces.Driver.OperationResult
      case Some(hash) =>
        val eventRef: RefVDR = RefVDR(hash)
        PRISMDriver.runProgram(
          for {
            ret <- vdrService.updateBytes(eventRef, vdrKey, data)
            (updateEventHash, signedPrismEvent, txHash) = ret
            out = Driver.OperationResult(
              updateEventHash.hex,
              Driver.OperationState.SUCCESS,
              Array(eventRef.eventHash.hex),
              Map(("h", eventRef.eventHash.hex)).asJava,
              null,
              null,
              null
            )
          } yield out
        )
  }

  override def delete(
      paths: Array[String],
      queries: java.util.Map[String, String],
      fragment: String,
      options: java.util.Map[String, ?]
  ): Unit = {
    paths.headOption match
      case None =>
        throw DataCouldNotBeFoundException(
          Some("the identifier is missing from the path")
        ) // interfaces.Driver.OperationResult
      case Some(hash) =>
        val eventRef: RefVDR = RefVDR(hash)
        PRISMDriver.runProgram(
          for {
            ret <- vdrService.deactivate(eventRef, vdrKey)
            (updateEventHash, signedPrismEvent, txHash) = ret
            out = Driver.OperationResult(
              updateEventHash.hex,
              Driver.OperationState.SUCCESS,
              Array(eventRef.eventHash.hex),
              Map(("h", eventRef.eventHash.hex)).asJava,
              null,
              null,
              null
            )
          } yield out
        )
  }

  override def read(
      paths: Array[String],
      queries: java.util.Map[String, String],
      fragment: String,
      publicKeys: Array[java.security.PublicKey]
  ): Array[Byte] = {
    paths.headOption match
      case None       => Array.empty()
      case Some(hash) =>
        val eventRef: RefVDR = RefVDR(hash)
        PRISMDriver.runProgram(
          for {
            vdr <- vdrService.fetch(eventRef)
          } yield vdr.data match {
            case VDR.DataEmpty()              => Array.empty()
            case VDR.DataDeactivated(data)    => Array.empty()
            case VDR.DataByteArray(byteArray) => byteArray
            case VDR.DataIPFS(cid)            => Array.empty()
            case VDR.DataStatusList(status)   => Array.empty()
          }
        )
  }

  /** in the case of PRISM this identifier is the HASH of the event */
  override def storeResultState(
      identifier: String
  ): interfaces.Driver.OperationState =
    OperationState.SUCCESS
    // OperationState.RUNNING //TODO since everything is a synchronous, the driver needs to keep a internal state for this
    // OperationState.ERROR //TODO only makes sense when the event submitted is not the latest version or if the submission didn't end up the the blockchain

  override def verify(
      paths: Array[String],
      queries: java.util.Map[String, String],
      fragment: String,
      publicKeys: Array[java.security.PublicKey],
      returnData: Boolean
  ): interfaces.Proof = {
    paths.headOption match
      case None       => ???
      case Some(hash) =>
        val eventRef: RefVDR = RefVDR(hash)
        PRISMDriver.runProgram(
          for { vdr <- vdrService.fetch(eventRef) } yield vdr.data match {
            case VDR.DataEmpty() =>
              Proof("PrismBlock", Array.empty(), Array.empty()) // TODO proof
            case VDR.DataDeactivated(data) =>
              data match {
                case VDR.DataEmpty()           => throw DataNotInitializedException()
                case VDR.DataDeactivated(data) =>
                  throw DataAlreadyDeactivatedException()
                case VDR.DataByteArray(byteArray) =>
                  Proof(
                    "PrismBlock",
                    byteArray, // Data
                    Array.empty() // TODO proof will is a protobuf Array of PRISM events. Reuse the PrismBlock?
                  )
                case VDR.DataIPFS(cid) =>
                  ??? // not part of the generic VDR specification
                case VDR.DataStatusList(status) =>
                  ??? // not part of the generic VDR specification
              }
            case VDR.DataByteArray(byteArray) =>
              Proof(
                "PrismBlock",
                byteArray, // Data
                Array.empty() // TODO proof will is a protobuf Array of PRISM events like a PrismBlock?
              )
            case VDR.DataIPFS(cid) =>
              ??? // not part of the generic VDR specification
            case VDR.DataStatusList(status) =>
              ??? // not part of the generic VDR specification
          }
        )
  }

}
