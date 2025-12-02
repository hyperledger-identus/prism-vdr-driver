package demo

import zio.*
import scala.jdk.CollectionConverters.*
import fmgp.util.bytes2Hex
import interfaces.Driver
import hyperledger.identus.vdr.prism.PRISMDriver

@main def step0 = {
  // 1.
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
    prismState <- ZIO.service[fmgp.did.method.prism.PrismState]
    _ = prismState.getSSI(DemoConfig.didPrism)
  } yield ())
}

@main def step1 = { // CREATE VDR ENTRY
  val driver = DemoConfig.createDriverMongoDBWithIndexer()
  println(s"✓ Driver Version: ${driver.getVersion}")
  println(s"✓ Driver Family: ${driver.getFamily}")
  println(s"✓ Driver ID: ${driver.getIdentifier}")

  println("STEP 1: Create Data")
  val originalData = "My Data. Just some bytes".getBytes
  val createResult = driver.create(originalData, Map.empty.asJava)
  val id = createResult.getIdentifier
  val vdrEntryId = id

  println(s"Data created: ${bytes2Hex(originalData)}'")
  println(s"VDREntryID: $vdrEntryId")
  println(s"Identifier: $id")
  println(s"State: ${createResult.getState}")
}

def vdrEntryId = ??? //TODO VDREntryID from step1

@main def step2 = { // READ VDR ENTRY
  val driver = DemoConfig.createDriverMongoDBWithIndexer()

  println("STEP 2: Read Data")
  val readData = driver.read(
    Array(vdrEntryId),
    Map.empty.asJava,
    null,
    Array.empty
  )
  println(s"Data read: ${bytes2Hex(readData)}")
  println(s"Data length: ${readData.length} bytes")
  // println(s"✓ Matches original: ${bytes2Hex(readData) == bytes2Hex(originalData)}")
}

@main def step3 = { // UPDATE VDR ENTRY
  val driver = DemoConfig.createDriverMongoDBWithIndexer()

  println("STEP 3: Updated Data")
  val updatedData = "Updated Data".getBytes
  val updateResult = driver.update(
    updatedData,
    Array(vdrEntryId),
    Map.empty.asJava,
    null,
    Map.empty.asJava
  )
  val newId = updateResult.getIdentifier

  // println(s"Old data: ${bytes2Hex(originalData)}")
  println(s"New data: ${bytes2Hex(updatedData)}")
  // println(s"Old identifier: $id")
  println(s"VDR Entry Id  : $vdrEntryId")
  println(s"New identifier: $newId")
  println(s"State: ${updateResult.getState}")
}

@main def step4 = step2 // READ VDR ENTRY

@main def step5 = { // Verify
  val driver = DemoConfig.createDriverMongoDBWithIndexer()

  println("STEP 5: Verify Data")
  val proof = driver.verify(
    Array(vdrEntryId),
    Map.empty.asJava,
    null,
    Array.empty,
    true
  )

  println(s"Proof Type: ${proof.getType}")
  println(s"Data: ${bytes2Hex(proof.getData)}")
  println(s"Proof bytes: ${proof.getProof.length}") // FIXME
  println("✓ Cryptographic proof obtained")
}

@main def step6 = { // Deactivate VDR Entry
  val driver = DemoConfig.createDriverMongoDBWithIndexer()

  println("STEP 6: Delete (deactivate) VDR Entry")
  driver.delete(
    Array(vdrEntryId),
    Map.empty.asJava,
    null,
    Map.empty.asJava
  )
  println("✓ Entry deactivated")
}

@main def step7 = step2 // READ VDR ENTRY

@main def step8 = {
  val driver = DemoConfig.createDriverMongoDBWithIndexer()
  val status = driver.storeResultState(vdrEntryId)
  status match {
    case Driver.OperationState.SUCCESS => println("✅ Operation succeeded")
    case Driver.OperationState.RUNNING => println("⏳ Operation in progress")
    case Driver.OperationState.ERROR   => println("❌ Operation failed")
  }
}
