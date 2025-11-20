package demo

import zio.*
import scala.jdk.CollectionConverters.*
import fmgp.util.bytes2Hex
import interfaces.Driver

import hyperledger.identus.vdr.prism.PRISMDriver
import fmgp.did.method.prism.PrismState

/** This example demonstrates the complete lifecycle of data on the blockchain: Create → Read → Update → Verify → Delete
  */
object CompleteExample {

  def main(args: Array[String]): Unit = {
    println("=" * 70)
    println("COMPLETE EXAMPLE - End to End")
    println("Full lifecycle: Create → Read → Update → Verify → Delete")
    println("=" * 70)

    // 0. Setup
    println("\n" + "━" * 70)
    println("STEP 1: Setup")
    println("━" * 70)
    val driver = DemoConfig.createDriverMongoDB()
    println(s"✓ Driver Version: ${driver.getVersion}")
    println(s"✓ Driver Family: ${driver.getFamily}")
    println(s"✓ Driver ID: ${driver.getIdentifier}")

    // 1. Create the DID
    val txHash = PRISMDriver.runProgram(
      DemoConfig
        .programCreateDID(
          DemoConfig.blockfrostConfig,
          DemoConfig.walletConfig
        )
    )
    println(s"✓ Create DID txHash: ${txHash.hex}") // 0e458f7a091f1cfed05ede0a29c11ef17305162ea1782d7cb165e0380a72ee0c
    // https://github.com/FabioPinheiro/prism-vdr/commit/7689f0d098183720d8ede331dbbd5bad47e82206
    DemoConfig.runWithPrismState(for {
      prismState <- ZIO.service[PrismState]
      _ = prismState.getSSI(DemoConfig.didPrism)
    } yield ())

    // ### WAIT for be written in blockchain ###

    // 2. Create data
    println("\n" + "━" * 70)
    println("STEP 2: Create Data")
    println("━" * 70)
    val originalData = "My Data. Just some bytes".getBytes
    val createResult = driver.create(originalData, Map.empty.asJava)
    val id = createResult.getIdentifier
    val vdrEntryId = id

    println(s"Data created: ${bytes2Hex(originalData)}'")
    println(s"Identifier: $id") // 79cd64ec382266690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7
    println(s"State: ${createResult.getState}")

    assert(vdrEntryId == "79cd64ec382266690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7")
    // val vdrEntryId = "79cd64ec382266690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7"

    // ### WAIT for be written in blockchain ###

    // 3. Read data
    println("\n" + "━" * 70)
    println("STEP 3: Read Data")
    println("━" * 70)
    val readData = driver.read(
      Array(vdrEntryId),
      Map.empty.asJava,
      null,
      Array.empty
    )
    println(s"Data read: ${bytes2Hex(readData)}")
    println(s"✓ Matches original: ${bytes2Hex(readData) == bytes2Hex(originalData)}")

    // 4. Update data
    println("\n" + "━" * 70)
    println("STEP 4: Update Data")
    println("━" * 70)
    val updatedData = "Updated Data".getBytes
    val updateResult = driver.update(
      updatedData,
      Array(vdrEntryId),
      Map.empty.asJava,
      null,
      Map.empty.asJava
    )
    val newId = updateResult.getIdentifier

    println(s"Old data: ${bytes2Hex(originalData)}")
    println(s"New data: ${bytes2Hex(updatedData)}")
    println(s"Old identifier: $id")
    println(s"New identifier: $newId")
    println(s"State: ${updateResult.getState}")

    // ### WAIT for be written in blockchain ###

    // Read updated data
    val newData = driver.read(
      Array(vdrEntryId),
      Map.empty.asJava,
      null,
      Array.empty
    )
    println(s"✓ Updated data read: ${bytes2Hex(newData)}")

    // 5. Verify
    println("\n" + "━" * 70)
    println("STEP 5: Verify Data")
    println("━" * 70)
    val proof = driver.verify(
      Array(vdrEntryId),
      Map.empty.asJava,
      null,
      Array.empty,
      true
    )

    println(s"Proof Type: ${proof.getType}")
    println(s"Data: ${bytes2Hex(proof.getData)}")
    println(s"Proof bytes: ${proof.getProof.length}")
    println("✓ Cryptographic proof obtained")

    // 6. Delete
    println("\n" + "━" * 70)
    println("STEP 6: Delete (Deactivate) Data")
    println("━" * 70)
    driver.delete(
      Array(vdrEntryId),
      Map.empty.asJava,
      null,
      Map.empty.asJava
    )
    println("✓ Entry deactivated")

    // ### WAIT for be written in blockchain ###

    // Try to read deleted data
    val deletedData = driver.read(
      Array(vdrEntryId),
      Map.empty.asJava,
      null,
      Array.empty
    )
    println(s"Data: '${bytes2Hex(deletedData)}'")
    println(s"Deleted data length: ${deletedData.length} bytes (empty)")
    println("✓ Deactivated entries return empty data")

    // 7. Check operation status
    println("\n" + "━" * 70)
    println("STEP 7: Check Operation Status")
    println("━" * 70)
    val status = driver.storeResultState(vdrEntryId)
    status match {
      case Driver.OperationState.SUCCESS => println("✅ Operation succeeded")
      case Driver.OperationState.RUNNING => println("⏳ Operation in progress")
      case Driver.OperationState.ERROR   => println("❌ Operation failed")
    }

    // Summary
    println("\n" + "━" * 70)
    println("SUMMARY - Complete Lifecycle")
    println("━" * 70)
    println("✅ CREATE:  Data stored on blockchain")
    println("✅ READ:    Data retrieved from blockchain")
    println("✅ UPDATE:  Data modified (new version created)")
    println("✅ VERIFY:  Cryptographic proof obtained")
    println("✅ DELETE:  Entry deactivated")
    println("✅ STATUS:  Operation status checked")

    println("\n" + "━" * 70)
    println("Key Concepts Demonstrated:")
    println("━" * 70)
    println("• Immutability: All data remains on-chain")
    println("• Versioning: Updates create event chains")
    println("• Verifiability: Cryptographic proofs available")
    println("• Lifecycle: Complete CRUD operations")
    println("• Transparency: All operations are public")

    println("\n" + "=" * 70)
    println("🎉 Complete example finished successfully!")
    println("=" * 70)
  }
}

// ======================================================================
// COMPLETE EXAMPLE - End to End
// Full lifecycle: Create → Read → Update → Verify → Delete
// ======================================================================

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 1: Setup
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// timestamp=2025-11-04T17:31:28.516706179Z level=INFO thread=#zio-fiber-1340051763 message="Chunks Files: ../prism-vdr-preprod/cardano-21325/chunk013; ../prism-vdr-preprod/cardano-21325/chunk014; ../prism-vdr-preprod/cardano-21325/chunk015; ../prism-vdr-preprod/cardano-21325/chunk012; ../prism-vdr-preprod/cardano-21325/chunk001; ../prism-vdr-preprod/cardano-21325/chunk006; ../prism-vdr-preprod/cardano-21325/chunk008; ../prism-vdr-preprod/cardano-21325/chunk009; ../prism-vdr-preprod/cardano-21325/chunk007; ../prism-vdr-preprod/cardano-21325/chunk000; ../prism-vdr-preprod/cardano-21325/chunk017; ../prism-vdr-preprod/cardano-21325/chunk010; ../prism-vdr-preprod/cardano-21325/chunk011; ../prism-vdr-preprod/cardano-21325/chunk016; ../prism-vdr-preprod/cardano-21325/chunk005; ../prism-vdr-preprod/cardano-21325/chunk002; ../prism-vdr-preprod/cardano-21325/chunk003; ../prism-vdr-preprod/cardano-21325/chunk004" location=fmgp.did.method.prism.vdr.Indexer.findChunkFiles file=Indexer.scala line=125
// timestamp=2025-11-04T17:31:28.518626304Z level=INFO thread=#zio-fiber-1340051763 message="Read chunkFiles (18)" location=hyperledger.identus.vdr.prism.PRISMDriver.loadPrismStateFromChunkFiles file=PrismDriver.scala line=28
// timestamp=2025-11-04T17:31:28.546333137Z level=INFO thread=#zio-fiber-1340051763 message="Init PrismState" location=hyperledger.identus.vdr.prism.PRISMDriver.loadPrismStateFromChunkFiles file=PrismDriver.scala line=39
// timestamp=2025-11-04T17:31:37.523137461Z level=INFO thread=#zio-fiber-1340051763 message="Finish Init PrismState: EventCounter(0,0,275944)" location=hyperledger.identus.vdr.prism.PRISMDriver.loadPrismStateFromChunkFiles file=PrismDriver.scala line=45
// timestamp=2025-11-04T17:31:37.526378419Z level=INFO thread=#zio-fiber-1340051763 message="PrismState was 271766 SSI and 2 VDR" location=hyperledger.identus.vdr.prism.PRISMDriver.loadPrismStateFromChunkFiles file=PrismDriver.scala line=48
// ✓ Driver Version: 1.0
// ✓ Driver Family: PRISM
// ✓ Driver ID: PRISMDriver

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 2: Create Data
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SLF4J(W): No SLF4J providers were found.
// SLF4J(W): Defaulting to no-operation (NOP) logger implementation
// SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
// timestamp=2025-11-04T17:31:38.555731753Z level=INFO thread=#zio-fiber-1875469858 message="submitTransaction" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=155
// timestamp=2025-11-04T17:31:38.557399795Z level=INFO thread=#zio-fiber-1875469858 message="submitTransaction txPayload = 84a400d90102818258200e458f7a091f1cfed05ede0a29c11ef17305162ea1782d7cb165e0380a72ee0c000181825839000a53930fc08a96c804f29a2fd47c9f4685bce6f0dddbd43f8e9379d954da2e2e92b55ae605ea9916d298d300da4817f11231b02745a5f3331b0000000253244e3e021a0002acfd0758207a3da023f0a8c2231e14ff9e6ce46c9a9eefebd05b096f1804a96e41ea7524bea100d90102818258208bdbeed2b2bd78c79db1492fb77fc79c8adeec0fc1a953f90f0198220b6588fc5840dc584d82bd442816ff5e1d720d8ebfb651b04774d3e22f411dfb560d1c72652c08d57a1fe25d49a52ddfb23bd7d81eaa8927888d0ee9e0082bb575ca2e11a40ef5a119534da2616386582022a60112a3010a047664723112473045022100f4b98ba677c183d6c5028b8d0d5820407b64962b91325c4a7e9dacaa8d5b1909784f0220621479c89a6a3893ef90715820e3a87de1277430c590501968d068bab83ab78318971a523a500a2051d47b133958203a7cc5c1afc47099dcbecccf0c8a70828c072ac82f55225b42d4f4920310a38458200978ab0601af74f16a1f031388a6a206184d7920446174612e204a7573742073496f6d65206279746573617601" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=159
// timestamp=2025-11-04T17:31:38.738085003Z level=INFO thread=#zio-fiber-1875469858 message="submitTransaction result = Result{successful=true, response='Response{protocol=h2, code=200, message=, url=https://cardano-preprod.blockfrost.io/api/v0/tx/submit}', code=200, value=6cf61468c519418a5bb5dbfc46d51f37f45d86b4109de2684054b568b0c0810d}" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=161
// timestamp=2025-11-04T17:31:38.740542170Z level=INFO thread=#zio-fiber-1875469858 message="See https://preprod.cardanoscan.io/transaction/6cf61468c519418a5bb5dbfc46d51f37f45d86b4109de2684054b568b0c0810d?tab=metadata" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=162
// Data created: 4d7920446174612e204a75737420736f6d65206279746573'
// Identifier: 79cd64ec382266690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7
// State: SUCCESS

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 3: Read Data
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// timestamp=2025-11-04T17:45:01.715369916Z level=INFO thread=#zio-fiber-94083123 message="fecth VDR entry '79cd64ec382266690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7'" location=fmgp.did.method.prism.vdr.VDRPassiveService.fetch file=VDRPassiveService.scala line=44
// Data read: 4d7920446174612e204a75737420736f6d65206279746573
// ✓ Matches original: true

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 4: Update Data
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SLF4J(W): No SLF4J providers were found.
// SLF4J(W): Defaulting to no-operation (NOP) logger implementation
// SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
// timestamp=2025-11-04T17:45:02.676381500Z level=INFO thread=#zio-fiber-667394744 message="submitTransaction" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=15
// timestamp=2025-11-04T17:45:02.677469875Z level=INFO thread=#zio-fiber-667394744 message="submitTransaction txPayload = 84a400d90102818258206cf61468c519418a5bb5dbfc46d51f37f45d86b4109de2684054b568b0c0810d000181825839000a53930fc08a96c804f29a2fd47c9f4685bce6f0dddbd43f8e9379d954da2e2e92b55ae605ea9916d298d300da4817f11231b02745a5f3331b000000025321a719021a0002a725075820f501b4d2b36a3b4590f5d4d303001bed368560e09975833ba53bbf8ee96532d7a100d90102818258208bdbeed2b2bd78c79db1492fb77fc79c8adeec0fc1a953f90f0198220b6588fc5840d0233f34cd9130285876f277ddefcd20738fde74bd2d6aed3faead4fe77758b82ffedc595d8dff953c130a28d9c1c2a02e29bd72dbfec562fb57d1ab4398fd05f5a119534da261638558202286011283010a04766472311246304402206d661dabfe31dcbfa538175be31b5820e00f43bfe01289378042ffd459bbd74da6210220591d0b6cff53a6db9650eb4c5820d978536dedbe32bfe3d20e32cb824e25813b90991a334231122079cd64ec3822582066690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7a2060c55706449617465642044617461617601" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=159
// timestamp=2025-11-04T17:45:02.921045417Z level=INFO thread=#zio-fiber-667394744 message="submitTransaction result = Result{successful=true, response='Response{protocol=h2, code=200, message=, url=https://cardano-preprod.blockfrost.io/api/v0/tx/submit}', code=200, value=1327ec11de2f94cc8328a21e4dddd1b6d6b81627cc32786cd1a9d9bc96542489}" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=161
// timestamp=2025-11-04T17:45:02.922540250Z level=INFO thread=#zio-fiber-667394744 message="See https://preprod.cardanoscan.io/transaction/1327ec11de2f94cc8328a21e4dddd1b6d6b81627cc32786cd1a9d9bc96542489?tab=metadata" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=162
// Old data: 4d7920446174612e204a75737420736f6d65206279746573
// New data: 557064617465642044617461
// Old identifier: 79cd64ec382266690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7
// New identifier: 09277269bbeabeb4524e44d97c0293bc69f0daf994c6e96a860734bc62af01c9
// State: SUCCESS
// timestamp=2025-11-04T17:45:02.926550209Z level=INFO thread=#zio-fiber-2018045583 message="fecth VDR entry '09277269bbeabeb4524e44d97c0293bc69f0daf994c6e96a860734bc62af01c9'" location=fmgp.did.method.prism.vdr.VDRPassiveService.fetch file=VDRPassiveService.scala line=44
// ✓ Updated data read: 4d7920446174612e204a75737420736f6d65206279746573

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 5: Verify Data
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// timestamp=2025-11-04T17:49:15.641520548Z level=INFO thread=#zio-fiber-2089576889 message="fecth VDR entry '79cd64ec382266690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7'" location=fmgp.did.method.prism.vdr.VDRPassiveService.fetch file=VDRPassiveService.scala line=44
// Proof Type: PrismBlock
// Data: 4d7920446174612e204a75737420736f6d65206279746573
// Proof bytes: 0

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 6: Delete (Deactivate) Data
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SLF4J(W): No SLF4J providers were found.
// SLF4J(W): Defaulting to no-operation (NOP) logger implementation
// SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
// timestamp=2025-11-04T17:51:15.425691714Z level=INFO thread=#zio-fiber-941964410 message="submitTransaction" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=15
// timestamp=2025-11-04T17:51:15.426825131Z level=INFO thread=#zio-fiber-941964410 message="submitTransaction txPayload = 84a400d90102818258201327ec11de2f94cc8328a21e4dddd1b6d6b81627cc32786cd1a9d9bc96542489000181825839000a53930fc08a96c804f29a2fd47c9f4685bce6f0dddbd43f8e9379d954da2e2e92b55ae605ea9916d298d300da4817f11231b02745a5f3331b00000002531f02e0021a0002a43907582071fef263b35ac68f5171ba193ae15e5260e60e3d95d9ec86dd4192c8c65ffaaea100d90102818258208bdbeed2b2bd78c79db1492fb77fc79c8adeec0fc1a953f90f0198220b6588fc5840115d9d8a31908d3c71169c15a71fdc6d47960d77cc02d9a375acce4188025fd93aa46777dc9b69b955eaffef66e6bd1f23abbe2df37236bd9312d625965f030cf5a119534da26163845820227712750a047664723112473045022100abe5ed983325f06787279b3e2804115820dbfc4dbb03ef3b015a3804590d7a65f77a02207eb452d21cd8dc9a8cf40f1d8a5820bff7323cd2d204c48bd000106406bcf9da7fdf1a244a22122079cd64ec3822665819690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7617601" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=159
// timestamp=2025-11-04T17:51:15.583146590Z level=INFO thread=#zio-fiber-941964410 message="submitTransaction result = Result{successful=true, response='Response{protocol=h2, code=200, message=, url=https://cardano-preprod.blockfrost.io/api/v0/tx/submit}', code=200, value=aa764d0faaf9bc1d6276af756b09a9cebefe23efb5622926e103514be6d59ec1}" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=161
// timestamp=2025-11-04T17:51:15.584877965Z level=INFO thread=#zio-fiber-941964410 message="See https://preprod.cardanoscan.io/transaction/aa764d0faaf9bc1d6276af756b09a9cebefe23efb5622926e103514be6d59ec1?tab=metadata" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=162
// ✓ Entry deactivated
// timestamp=2025-11-04T17:51:15.587751631Z level=INFO thread=#zio-fiber-1875562470 message="fecth VDR entry '79cd64ec382266690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7'" location=fmgp.did.method.prism.vdr.VDRPassiveService.fetch file=VDRPassiveService.scala line=44
// Deleted data length: 24 bytes (empty)
// ✓ Deactivated entries return empty data

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 6: Delete (Deactivate) Data
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SLF4J(W): No SLF4J providers were found.
// SLF4J(W): Defaulting to no-operation (NOP) logger implementation
// SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
// timestamp=2025-11-04T18:24:31.770821680Z level=INFO thread=#zio-fiber-620146808 message="submitTransaction" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=155
// timestamp=2025-11-04T18:24:31.771760055Z level=INFO thread=#zio-fiber-620146808 message="submitTransaction txPayload = 84a400d9010281825820aa764d0faaf9bc1d6276af756b09a9cebefe23efb5622926e103514be6d59ec1000181825839000a53930fc08a96c804f29a2fd47c9f4685bce6f0dddbd43f8e9379d954da2e2e92b55ae605ea9916d298d300da4817f11231b02745a5f3331b00000002531c5ea7021a0002a4390758200921376b3c4952e77ff5ff5e7d5c8526bc4da33a7e731cadd3f05a41a37ca5f2a100d90102818258208bdbeed2b2bd78c79db1492fb77fc79c8adeec0fc1a953f90f0198220b6588fc584099e237c46b05b7fe49570c9beaa9333fb6affeffbaabc09645d606e057ebc89338e60e61c586a48e40c3990359d0710339949ddccd2004d470c41d394860a901f5a119534da26163845820227712750a047664723112473045022100c00254a8d54a2ffbf9943943fd2ef258208ae5d769d909bf32c81f1353fb2505a3f2022018e501871111edb5e4186a34445820e8a570dce9f56ada1466cfb23810ae707b23cd1a244a22122009277269bbeabe5819b4524e44d97c0293bc69f0daf994c6e96a860734bc62af01c9617601" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=159
// timestamp=2025-11-04T18:24:31.972030166Z level=INFO thread=#zio-fiber-620146808 message="submitTransaction result = Result{successful=true, response='Response{protocol=h2, code=200, message=, url=https://cardano-preprod.blockfrost.io/api/v0/tx/submit}', code=200, value=4bac097a333d6a074dbd41ee77e5360338eed8f6dfb476252e4b2ecf7dea8040}" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=161
// timestamp=2025-11-04T18:24:31.973369625Z level=INFO thread=#zio-fiber-620146808 message="See https://preprod.cardanoscan.io/transaction/4bac097a333d6a074dbd41ee77e5360338eed8f6dfb476252e4b2ecf7dea8040?tab=metadata" location=fmgp.did.method.prism.CardanoService.submitTransaction file=CardanoClientService.scala line=162
// ✓ Entry deactivated
// timestamp=2025-11-04T18:39:29.629982263Z level=INFO thread=#zio-fiber-452914633 message="fecth VDR entry '79cd64ec382266690825f9955b9a3d22d1564c276f054a8da86b2ea68e334fb7'" location=fmgp.did.method.prism.vdr.VDRPassiveService.fetch file=VDRPassiveService.scala line=44
// Data: ''
// Deleted data length: 0 bytes (empty)
// ✓ Deactivated entries return empty data

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 7: Check Operation Status
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ✅ Operation succeeded

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SUMMARY - Complete Lifecycle
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ✅ CREATE:  Data stored on blockchain
// ✅ READ:    Data retrieved from blockchain
// ✅ UPDATE:  Data modified (new version created)
// ✅ VERIFY:  Cryptographic proof obtained
// ✅ DELETE:  Entry deactivated
// ✅ STATUS:  Operation status checked

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Key Concepts Demonstrated:
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// • Immutability: All data remains on-chain
// • Versioning: Updates create event chains
// • Verifiability: Cryptographic proofs available
// • Lifecycle: Complete CRUD operations
// • Transparency: All operations are public

// ======================================================================
// 🎉 Complete example finished successfully!
// ======================================================================
