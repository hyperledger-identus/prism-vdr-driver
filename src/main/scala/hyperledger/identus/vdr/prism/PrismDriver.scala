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

/** Shared utilities for PRISM VDR Driver implementations
  *
  * This object contains common methods used by both PRISMDriverInMemory and PRISMDriverMongoDB.
  */
object PRISMDriver {

  /** Execute a ZIO program synchronously
    *
    * Helper method to run ZIO effects in a synchronous context.
    *
    * @param program
    *   The ZIO program to execute
    * @return
    *   The result of the program execution
    */
  def runProgram[E, A](program: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(program).getOrThrowFiberFailure()
    }

}

trait PRISMDriver extends PRISMReadOnlyDriver {

  def run[E, A](program: ZIO[VDRService, E, A]): A
  protected def runWithVDRPassiveService[E, A](program: ZIO[VDRPassiveService, E, A]): A = run(program)

  override def getIdentifier: String // = "PRISMDriver"
  def didPrism: DIDPrism
  def vdrKey: Secp256k1PrivateKey

  override def getFamily: String = "PRISM"
  override def getVersion: String = "1.0"
  override def getSupportedVersions: Array[String] = Array("1.0")

  override def create(
      data: Array[Byte],
      options: java.util.Map[String, ?]
  ): interfaces.Driver.OperationResult = {
    run(
      for {
        vdrService <- ZIO.service[VDRService]
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
        run(
          for {
            vdrService <- ZIO.service[VDRService]
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
        run(
          for {
            vdrService <- ZIO.service[VDRService]
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
}
