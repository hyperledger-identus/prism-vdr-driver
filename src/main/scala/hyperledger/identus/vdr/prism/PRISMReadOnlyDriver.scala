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

trait PRISMReadOnlyDriver extends Driver {

  protected def runWithVDRPassiveService[E, A](program: ZIO[VDRPassiveService, E, A]): A

  override def getIdentifier: String // = "PRISMDriver"
  def didPrism: DIDPrism
  def vdrKey: Secp256k1PrivateKey

  override def getFamily: String = "PRISM"
  override def getVersion: String = "1.0"
  override def getSupportedVersions: Array[String] = Array("1.0")

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
        runWithVDRPassiveService(
          for {
            vdrService <- ZIO.service[VDRPassiveService]
            vdr <- vdrService.fetch(eventRef)
          } yield vdr.data match {
            case VDR.DataEmpty()              => Array.empty[Byte]()
            case VDR.DataDeactivated(data)    => Array.empty[Byte]()
            case VDR.DataByteArray(byteArray) => byteArray
            case VDR.DataIPFS(cid)            => Array.empty[Byte]()
            case VDR.DataStatusList(status)   => Array.empty[Byte]()
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
        runWithVDRPassiveService(
          for {
            vdrService <- ZIO.service[VDRPassiveService]
            vdr <- vdrService.fetch(eventRef)
          } yield vdr.data match {
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

  override def create(
      data: Array[Byte],
      options: java.util.Map[String, ?]
  ): interfaces.Driver.OperationResult = ??? // intentional it should return an error

  override def update(
      data: Array[Byte],
      paths: Array[String],
      queries: java.util.Map[String, String],
      fragment: String,
      options: java.util.Map[String, ?]
  ): interfaces.Driver.OperationResult = ??? // intentional it should return an error

  override def delete(
      paths: Array[String],
      queries: java.util.Map[String, String],
      fragment: String,
      options: java.util.Map[String, ?]
  ): Unit = ??? // intentional it should return an error
}
