package demo

import zio.*
import fmgp.did.method.prism.*
import fmgp.did.method.prism.cardano.*
import fmgp.did.method.prism.vdr.VDRService
import fmgp.crypto.Secp256k1PrivateKey
import hyperledger.identus.vdr.prism.*

/** Shared configuration for all demo examples
  *
  * This object provides a centralized configuration for the PRISM VDR Driver demos, avoiding code duplication across
  * examples.
  */
object DemoConfig {

  /** Blockfrost API configuration for preprod (testnet) */
  val blockfrostConfig: BlockfrostConfig = BlockfrostConfig(token = Secrets.blockfrostToken)
  // FIXME rotate key

  /** Working directory for driver state management */
  val workdir: String = "../prism-vdr-preprod"

  /** Cardano wallet configuration with 24-word mnemonic
    *
    * ⚠️ WARNING: This is a test wallet for demo purposes only! Never use this wallet for real funds or commit real
    * mnemonics to git!
    */
  val walletConfig: CardanoWalletConfig = CardanoWalletConfig(
    Seq(
      "mention",
      "side",
      "album",
      "physical",
      "uncle",
      "lab",
      "horn",
      "nasty",
      "script",
      "few",
      "hazard",
      "announce",
      "upon",
      "group",
      "ten",
      "moment",
      "fantasy",
      "helmet",
      "supreme",
      "early",
      "gadget",
      "curve",
      "lecture",
      "edge"
    )
  )

  /** DID PRISM identifier
    *
    * This is the decentralized identifier that owns the VDR entries.
    */
  val didPrism: DIDPrism = DIDPrism("51d47b13393a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4")

  def programCreateDID(
      bfConfig: BlockfrostConfig,
      wallet: CardanoWalletConfig,
  ) = {
    val pkMaster = walletConfig.secp256k1PrivateKey(0, 0)
    val pk1VDR = walletConfig.secp256k1PrivateKey(0, 1)
    val (tmpDIDPrism, signedPrismEvent) = DIDExtra.createDID(Seq(("master1", pkMaster)), Seq(("vdr1", pk1VDR)))
    assert(tmpDIDPrism.string == didPrism.string)
    for {
      txHash <- PrismChainServiceImpl(bfConfig, wallet).commitAndPush(
        Seq(signedPrismEvent),
        Some("DID from PrismVdrDemo")
      )
      _ <- ZIO.log(s"DID '${tmpDIDPrism.string}' created in txHash '${txHash.hex}'")
    } yield (txHash)
  }

  /** VDR signing key derived from wallet
    *
    * This key is used to sign all VDR operations (create, update, delete).
    */
  val vdrKey: Secp256k1PrivateKey =
    Secp256k1PrivateKey(walletConfig.secp256k1PrivateKey(0, 1).rawBytes)

  /** Key name of the VDR key type */
  // val keyName: String = "vdr1"

  def runWithPrismState[E, A](program: ZIO[PrismState, E, A]) = {
    import fmgp.did.method.prism.mongo.AsyncDriverResource
    val layer =
      AsyncDriverResource.layer >>> PrismStateMongoDB.makeLayer(Secrets.mongoDBConnection)
    PRISMDriver.runProgram(program.provideLayer(layer))
  }

  /** Create a configured PRISM Driver instance
    *
    * @return
    *   A configured PRISMDriverInMemory ready to use
    */
  def createDriverInMemory(): PRISMDriverInMemory =
    PRISMDriverInMemory(
      blockfrostConfig = blockfrostConfig,
      wallet = walletConfig,
      didPrism = didPrism,
      vdrKey = vdrKey,
      workdir = workdir
    )

  def createDriverMongoDBWithIndexer(): PRISMDriverMongoDBWithIndexer =
    PRISMDriverMongoDBWithIndexer(
      blockfrostConfig = blockfrostConfig,
      wallet = walletConfig,
      didPrism = didPrism,
      vdrKey = vdrKey,
      mongoDBConnection = Secrets.mongoDBConnection
    )

  /** Print configuration information (without sensitive data) */
  def printInfo(): Unit = {
    println("PRISM VDR Demo Configuration")
    println("=" * 60)
    println(s"Network:     Cardano Preprod (Testnet)")
    println(s"DID:         ${didPrism.string}")
    // println(s"Key Name:    $keyName")
    println(s"Work Dir:    $workdir")
    println("=" * 60)
  }
}
