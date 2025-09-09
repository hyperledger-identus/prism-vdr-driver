# Cardano PRISM VDR Specification

## Status of this document
 This document is a draft proposal for a Verifiable Data Registry (VDR). The document is in constant evolution, and future versions are under development. Implementers are encouraged to follow the evolution of the new versions closely to understand, provide feedback, and suggest changes.

## Abstract

This document defines a protocol for a Verifiable Data Registry (VDR) that uses the Cardano blockchain as the source of truth.
It uses the blockchain transaction metadata to record verifiable information that can be updated by entities possessing the associated cryptographic keys.

The fundamental building block of this VDR is the creation of a Self-Sovereign Identity (SSI) on the Cardano blockchain.
This SSI-based model is as decentralized and distributed as the underlying Cardano ledger itself.

This VDR protocol specifies events, serialization formats, and lifecycle management rules for the VDR entries.
A VDR node or VDR indexer refers to software implementing this specification.

This protocol has the same foundation already used by the [`did:prism` DID method](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md), which follows the [DID Core Specification](https://www.w3.org/TR/did-core/) to define Decentralized Identifiers (DIDs) on top of this Cardano VDR.

We would like to remark that any reference to the blockchain, such as "Cardano network", "underlying chain", "on-chain", "ledger", "the blockchain", and similar ones throughout this document, refers to the Cardano mainnet unless explicitly said otherwise.
The prism DID method, in its current form, solely depends on and uses the Cardano mainnet.

## Protocol parameters

Parameter            | Value             | Description
-------------------- | ----------------- | -----------
NETWORK              | `mainnet`         | Cardano blockchain network
PRISM_LABEL          | `21325`           | Cardano metadata label used by this protocol
CRYPTOGRAPHIC_CURVE	 | `secp256k1`       | Cryptographic curve used with a key.
SIGNATURE_ALGORITHM	 | SHA256 with ECDSA | Asymmetric public key signature algorithm.
HASH_ALGORITHM       | SHA-256           | Algorithm for generating hashes.
SECP256K1_CURVE_NAME | `secp256k1`       | String identifier for the SECP256K1 elliptic curve
ED25519_CURVE_NAME	 | `Ed25519`         | String identifier for the ED25519 elliptic curve
X25519_CURVE_NAME	   | `X25519`          | String identifier for the Curve25519 elliptic curve

Notes: 
- `PRISM_LABEL` is used by:
  - `did:prism` method - [prism-did-method-spec](https://github.com/input-output-hk/prism-did-method-spec/).
  - PRISM CredentialBatch - Specification is not public.
  - Anyone else who wants to, for whatever reason.
- The `CRYPTOGRAPHIC_CURVE` is the key type used by the master key and vdr key.
  It follows the guidance of [CIP-0016 - Cryptographic Key Serialisation Formats](https://github.com/cardano-foundation/CIPs/tree/master/CIP-0016)

## Events

The PRISM VDR is composed of a sequence of **immutable** events.
As the source of truth and order, the events are permanently recorded in the Cardano (mainnet) blockchain transaction metadata.
Multiple events can be recorded in a single blockchain transaction.
Events can be either independent or dependent. Forming a chain from previous events.
Events can be considered valid or invalid, according to the validation algorithm for each category of events.
Invalid events are ignored. 
Valid events can create a reference or a chain to update the state of a reference. (a `did:prism:...` or a storage entry is an example of references).
The chain of events is intended to update the state of the reference, but doesn't necessarily need to update the state to be a valid event.

There are several categories of events representing different entries in this VDR:
- `SSI` - Self-Sovereign Identity of [**did:prism**](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md).
- IssueCredentialBatch - Legacy ATALA PRISM format (for credential revocation)
- `Storage` - Stores a small amount of information (limited by Cardano metadata constraints), that represents mutable data.
- Future extensions — Any future extension MUST maintain **backward** and **forward** compatibility

Events may depend on others (e.g., a Storage Entry must be linked to an SSI entry).

All events follow an order. The timeline in which the event occurs in the blockchain.
- Events follow blockchain transaction order.
- Within a transaction, events follow the order they appear in PrismBlock.

### Encoding format

All data is encoded in protobuf to ensure backward and forward compatibility.

In this document, we use the **notation:** `E-x-y-z` to denote paths in the data model (E = root event, x/y/z = field indices).

### SSI entry

The fundamental data structure of the VDR is the `SSI` entries.
Any Self-sovereign identity (`SSI`) entry represents a `DID`.
It also contains additional information that is not part of the `DID` standard. Such as the `MASTER_KEY` and the `VDR_KEY`.

The conversion from an `SSI` entry to a `DID Document` as specified by [DID Core](https://www.w3.org/TR/did-1.0/), follows the [**did:prism**](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md) specification.

`DID`s are typically anchored on an underlying system or network. Regardless of the specific technology, any system that can record `DID`s and provide the data required to construct a corresponding `DID Document` is called a Verifiable Data Registry (`VDR`) - https://www.w3.org/TR/did-core/#detailed-architecture-diagram.
In the case of the `did:prism` method, identifiers are recorded in the `PRISM VDR`, which is defined by the protocol described in this specification.
The `SSI` entries are only one of the categories of event types supported by this VDR.

#### SSI events type
There are three types of events for `SSI` entries.

Event Name     | Protobuf | Description
-------------- | -------- |-------------
Create SSI     | `E-1`    | Create a new `SSI` (and `DID`) identifier.
Update SSI     | `E-2`    | Builds a chain of `SSI` events to update the state of the `SSI` identifier.
Deactivate SSI | `E-6`    | Deactivate `SSI` identifier. This is an end stat and can no longer be activated.

```mermaid
---
title: SSI entry state machine
---
stateDiagram-v2
    Nonexistent: Nonexistent identifier
    Active: Active SSI/DID
    Deactivated: Deactivated SSI
    [*] --> Nonexistent
    Nonexistent --> Active: E-1
    Active --> Active: E-2
    Active --> Deactivated: E-6
```

#### Validation rules:
- The `E-1` event MUST be signed with the private part of the `MASTER_KEY` it specifies.
- The identifier of the `SSI` MUST be the hash of the `E-1` event (using `HASH_ALGORITHM`) serves as the unique SSI reference.
- The `E-2` and `E-6` events MUST be signed by a `MASTER_KEY` listed in the `SSI` at that point in time.
- The `E-2-1` and `E-6-1` events MUST point to the `previous_event_hash`. The reference (hash) of the most recent event that was used to create or update the SSI.
- The `MASTER_KEY` MUST use the curve `SECP256K1_CURVE_NAME`.
-  Note:
   The `did:prism` also has a long form, in which the source of truth might not be on the blockchain.
   <br> The long form identifier is related to a canonical form identifier.
   <br> If the canonical identifier is not on the blockchain, then the long form is self-contained.
   <br> The `E-1` pure protobuf (without being warped in a signed event) is encoded from the binary representation of the protobuf into `base64URL` and then appended to the end of the canonical form, separated by the character `:`.
   <br> See [**did:prism** specification](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md) for more information.


#### Algorithm generate DID Document from SSI

Note: Storage entry depends on the SSI.

For more documentation see [**prism:did specs**](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md).

To summarize, a DID is valid only when its underlying SSI has been created `#E-1` and not deactivated `#E-6`.
The DID Document is a simplified representation of the SSI's latest status.
That does not contain the `MASTER_KEY`; `REVOCATION_KEY`; `VDR_KEY`.

Note: There are some cases where the SSI entry is not used as a DID. For example, if you care about managing Storage Entry.
The Prism Indexer MUST still be able to resolve that DID, even if the only field in the DID Document is the `id` of the DID itself.

So `{"id":"did:prism:00592a141a4c2bcb7a6aa691750511e2e9b048231820125e15ab70b12a210aae"}` it's a valid DID Document.

### Storage entry

The Storage entries allow for the recording of a small amount of information that represents mutable data.
The recorded information is persistent, public, and is limited by the constraints of Cardano metadata.

The Storage Entries form a chain like SSI entries (create → update ... → deactivate)

There are three types of events over **Storage Entries** (see protobuf definition):
Event Name               | Protobuf | Description
------------------------ | -------- |-------------
Create Storage entry     | `E-7`    | Create a new storage entry. Where the identifier is the `HASH_ALGORITHM` of the protobuf.
Update Storage entry     | `E-8`    | Builds a chain on a storage entry to update the state of that entry.
Deactivate Storage entry | `E-9`    | Deactivate the storage entry. This is an end stat and can no longer be activated.

Note:
The amount of information stored in a Storage Entry should be relatively small.
The larger the amount of data, the more expensive it will be to submit as metadata in a transaction.
Additionally, transaction metadata has hard limits.

When a Storage Entry is created, the first field, `E-7-1`, refers to the identity `SSI` of the creator of the Storage Entry.
The Storage Entry is also designed as a chain, similar to the `SSI` entries. The creator (`E-7-1`) is allowed to send follow-up events to update the content of the entry.

Like the `SSI`, the `**Storage Entry**`, all events can be referenced by the hash (`HASH_ALGORITHM`). But the identifier of the entry is the reference to the first event of the chain.
The create storage event `E-7` should include a nonce (`E-7-50`) to allow for the creation of multiple distinct entries with the same initial information.
Example: For use cases like the **StatusList**, where the initial value may be identical (e.g., an array of zeros representing the validity of a credential), the nonce (`E-7-50`) ensures the creation of multiple distinct Storage Entries.

Also, like the `SSI`, it should be possible to resolve the Storage Entry to a point in time.
By default, when resolving the Storage Entry, it SHOULD return the latest state of the entry.

```mermaid
---
title: Storage entry state machine
---
stateDiagram-v2
    classDef yourState font-style:italic,font-weight:bold,stroke:yellow

    Nonexistent: Non-existent identifier
    Active: Active VDR entry
    Deactivated: Deactivated VDR entry
    UnknownState:::yourState: Unknown State 
    [*] --> Nonexistent
    Nonexistent --> Active: E-7
    Active --> Active: E-8
    Active --> Deactivated: E-9
    Active --> Deactivated: E-6
    Active --> UnknownState: E-8 or E-9 (with unknown validation field)
    Nonexistent --> UnknownState: E-7 (with unknown validation field)
    UnknownState --> UnknownState: E-8 and E-9
    UnknownState --> Deactivated: E-6
```

The not no Unknown State only exists on the PRISM Indexer implementation. (See the section #Indexer)
<br> It covers the future extension of this protocol and represents either an Active or Deactivated state.

#### Validation rules
For the Storage Events (`E-7`, `E-8`, `E-9`) to be considered valid, they must meet the following criteria:
- The event must have a valid signature.
- All protobuf fields from `1...49` MUST be considered in the validation process in `E-7`, `E-8`, `E-9`.
  - At least one field must be present.
  - Any missing field from the port above is considered valid.
  - If `E-7-1` is present, the event must be signed by the owner's key, using:
    - the state of the `SSI` the moment before the event. (all and only the events before the one that is being validated)
    - the `SIGNATURE_ALGORITHM`
    - the curve `SECP256K1_CURVE_NAME`
    - the purpose `#KeyUsage-8` (VDR_KEY)
  - In `E-8`, `E-9` fields `2`(`E-8-2`) and `3`(`E-9-2`) must be present and point to the reference of the latest valid event for that identitier.
  - Field `E-7-3...49` is reserved for future extension of this protocol.
    <br> (Note the PRISM Indexer can still index and serve a requester all raw events about any storage entry without validating them or resolving the content of the data. See section below)
- The keys used must be present in the SSI at the time of the event. (Key rotation after the event does not invalidate the Storage Entries.)
- The `SSI` owner (`E-7-1`) must not be deactivated. Otherwise, the **Storage Entries** SHOULD be considered deactivated.

**Notes about the protobuf structure:**
- For convenience, the three messages share a common subset of fields. This ensures that if the protobuf messages were merged, the field positions and their meanings would remain consistent.
- The fields from `1` to `49` of the protobuf message are used for validation.
  Although at the moment only field 1 is defined, at the time of writing.
  - Field `1` is the owner of the entry - the specificId of the `did:prism`.
- The fields from `50` to `99` of the protobuf message are used for metadata.
  Although at the moment only field 50 is defined, at the time of writing.
  - Field `50` is a nonce - can be used to generate a different reference hash in the create event
    It allowed for making different entries with the same initial data possible.


The distinction between metadata and validation fields is important for any VDR PRISM Indexer implementation.
If the validation fields are present, they MUST be considered for the validation on the event.
In the case the PRISM Indexer doesn't know the rules for the field, it cannot consider the event valid or invalid.
Consequently, it cannot resolve the content of the data if any of those skills are defined in a chain of events

**Future extensions to this document may introduce more ways to encode data on the storage Entries.**
If the PRISM Indexer encounters an `UNKNOWN` encoded type of data in the protobuf fields of events `E-7` and `E-8`, it may ignore the content of that particular Storage entry.
However, the existence of the Storage entry must still be recognized, verified, indexed, and any related events should be returned by the PRISM Indexer. In cases of unsupported types, the resolved content can be left blank or empty.

Users of the indexer SHOULD always have the opportunity to retrieve all associated raw events. So they can locally verify and resolve the content.  

The encode method of the Storage Entry may be defined by the create event/operation, depending on the protobuf field used:
 The data, the following types of information/data may be stored:

Type of Data                    | Protobufs           | Represents
------------------------------- | ------------------- |-------------
 **bytes**                      | `E-7-100`,`E-8-100` | A raw array of bytes.
**CID (content identifier)**    | `E-7-101`,`E-8-101` | A reference to an IPFS document.


##### Storage Entry - bytes (`E-7-100` & `E-8-100`)

This data type is designed to represent an array of bytes.
The field `E-7-100` is the initial array of bytes (protobuf type `bytes`) that the content of this entry will have.
The field `E-8-100` is an array of bytes (protobuf type `bytes`) and will replace the previous content of the state with the new.

##### Storage Entry - IPFS CID (`E-7-101` & `E-8-101`)

This data type represented the **CID (content identifier)** of a document that should be on the **Interplanetary File System (IPFS)**. 

Note: 
This protocol (PRISM VDR) only stores a CID as the content.
The real content should be retrieved from an IPFS Gateway.

**Security considerations** - 
The **CID** is a reference to immutable data but also a hash of the data itself.
Therefore, it is recommended that when retrieving the data and verify that this is the actual data corresponding to that CID.

**Data Persistence considerations** -
Since **IPFS** relies on nodes voluntarily storing data, there is no guarantee that data will be stored permanently.
For some use cases, the actors may consider pinning the data themselves to guarantee data persistence.


## Indexer

Design goals:
- **Determinism**
  <br> The Indexer MUST be deterministic.
- **Rollback capability**
  <br>Indexer SHOUD be able to revert all steps to a previous `Cardano Block`.
  <br>This is because the Ouroboros protocol used in Cardano and its eventual consensus property.
  - Ideally, we recommend that the indexer be able to backtrack all steps and unapply them.
  - Can also run with a delay `k` (`Settlement time`).
    - At the time of of writing `k=2160` (36 minutes).
    - In practice, the network is considered very stable:
      - 15–30 blocks (~15–30 seconds) for light confirmation.
      - ~150–300 blocks (~2.5–5 minutes) for strong practical finality.
- **Event organization**
  <br>The main goal of the Indexer is to organize events into chains, so that the event chain for any particular identifier can be efficiently retrieved.
- **Signature validation**
  <br>It is not the responsibility of the Indexer to validate the signatures of PRISM Events.
- **General-purpose design**
  <br>The Indexer is described for the general use case.


## Document History

0. Initial draft.
1. Restructuring the document, Fix typos and incorporate recommendations from reviews.

## Authors and Contributors

- **Fabio Pinheiro** <br>
  [Email: fabio.pinheiro@iohk.io](mailto:fabio.pinheiro@iohk.io) <br>
  [Github: FabioPinheiro](https://github.com/FabioPinheiro)

---

## Appendices

### Events protobuf structure

```mermaid
classDiagram

class PrismBlock
PrismBlock : #1 SignedPrismEvent - events

class SignedPrismEvent
SignedPrismEvent : #1 - string signed_with // The key ID used to sign the event, it must belong to the DID that signs the event.
SignedPrismEvent : #2 - bytes signature // The actual signature.
SignedPrismEvent : #3 - PrismEvent - event // The event that was signed.

```
```mermaid
classDiagram

class PrismEvent


PrismEvent : SSI
PrismEvent : #E-1 - ProtoCreateDID create_did
PrismEvent : #E-2 - ProtoUpdateDID update_did
PrismEvent : #E-6 - ProtoDeactivateDID deactivate_did
PrismEvent : Storage
PrismEvent : #E-7 - ProtoCreateStorageEntry create_storage_entry
PrismEvent : #E-8 - ProtoUpdateStorageEntry update_storage_entry
PrismEvent : #E-9 - ProtoDeactivateStorageEntry deactivate_storage_entry
PrismEvent : ---
PrismEvent : #E-3 - IssueCredentialBatchOperation issue_credential_batch
PrismEvent : #E-4 - RevokeCredentialsOperation revoke_credentials
PrismEvent : #E-5 - ProtocolVersionUpdateOperation protocol_version_update

```

```mermaid
classDiagram

class ProtoCreateDID
ProtoCreateDID : #E-1-1 - DIDCreationData did_data
DIDCreationData : #E-1-1-2 - PublicKey public_keys
DIDCreationData : #E-1-1-3 - Service services
DIDCreationData : #E-1-1-4 - string context

```

```mermaid
classDiagram
class ProtoUpdateDID
ProtoUpdateDID : #E-2-1 - bytes previous_operation_hash
ProtoUpdateDID : #E-2-2 - string id
ProtoUpdateDID : #E-2-3 - [UpdateDIDAction] actions

class UpdateDIDAction
UpdateDIDAction : #E-3-1 - AddKeyAction add_key // Used to add a new key to the DID.
UpdateDIDAction : #E-3-2 - RemoveKeyAction remove_key // Used to remove a key from the DID.
UpdateDIDAction : #E-3-3 - AddServiceAction add_service // Used to add a new service to a DID.
UpdateDIDAction : #E-3-4 - RemoveServiceAction remove_service // Used to remove an existing service from a DID.
UpdateDIDAction : #E-3-5 - UpdateServiceAction update_service // Used to update a list of service endpoints of a given service on a given DID.
UpdateDIDAction : #E-3-6 - PatchContextAction patch_context // Used to update a list of '@context' strings used during resolution for a given DID.
```

```mermaid
classDiagram
class ProtoDeactivateDID
ProtoDeactivateDID : #E-6-1 - bytes previous_operation_hash
ProtoDeactivateDID : #E-6-2 - string id // DID Suffix of the DID to be deactivated
```

```mermaid
classDiagram
class ProtoCreateStorageEntry
ProtoCreateStorageEntry: #E-7-1 did_prism_hash - The specificId of the did prism.
ProtoCreateStorageEntry: #E-7-50 nonce - The specificId of the did prism.

ProtoCreateStorageEntry: #E-7-100 bytes - (DATA) represents an encoded array of bytes
ProtoCreateStorageEntry: #E-7-101 ipfs - (DATA) represents an encoded CID
```

```mermaid
classDiagram
class ProtoUpdateStorageEntry
ProtoUpdateStorageEntry: #E-8-2 previous_event_hash - The hash of the most recent event of VDR entry.

ProtoUpdateStorageEntry: #E-8-100 bytes - (DATA) represents an encoded array of bytes
ProtoUpdateStorageEntry: #E-8-101 ipfs - (DATA) represents an encoded CID
```


```mermaid
classDiagram
class ProtoDeactivateDID
ProtoDeactivateDID : #E-7-2 previous_event_hash - The hash of the most recent event of VDR entry.
```

```
classDiagram
class KeyUsage
KeyUsage : #KeyUsage-0 UNKNOWN_KEY
KeyUsage : #KeyUsage-1 MASTER_KEY
KeyUsage : #KeyUsage-2 ISSUING_KEY
KeyUsage : #KeyUsage-3 KEY_AGREEMENT_KEY
KeyUsage : #KeyUsage-4 AUTHENTICATION_KEY
KeyUsage : #KeyUsage-5 REVOCATION_KEY
KeyUsage : #KeyUsage-6 CAPABILITY_INVOCATION_KEY
KeyUsage : #KeyUsage-7 CAPABILITY_DELEGATION_KEY
KeyUsage : #KeyUsage-8 VDR_KEY
```


### File 'prism.proto'

```proto
/**
 * Wraps an PrismBlock and its metadata.
 */
message PrismObject {
  reserved 1, 2, 3; 
  reserved "block_hash";
  reserved "block_event_count"; // Number of events in the block.
  reserved "block_byte_length"; // Byte length of the block.
  
  PrismBlock block_content = 4; // The block content.
}

/**
 * Represent a block that holds events.
 */
 message PrismBlock {
  reserved 1; // Represents the version of the block. Deprecated
  repeated SignedPrismEvent events = 2; // A signed event, necessary to post anything on the blockchain.
  }
  
// A signed event, necessary to post anything on the blockchain.
message SignedPrismEvent {
  string signed_with = 1; // The key ID used to sign the event, it must belong to the DID that signs the event.
  bytes signature = 2; // The actual signature.
  PrismEvent event = 3; // The event that was signed.
}


// The possible events affecting the blockchain.
message PrismEvent {
  // https://github.com/input-output-hk/atala-prism-sdk/blob/master/protosLib/src/main/proto/node_models.proto
  //  reserved 3, 4; // fields used by an extension of the protocol. Not relevant for the DID method
  // The actual event.
  oneof event {
    // Used to create a public DID.
    ProtoCreateDID create_did = 1;

    // Used to update an existing public DID.
    ProtoUpdateDID update_did = 2;

    // Used to issue a batch of credentials.
    ProtoIssueCredentialBatch issue_credential_batch = 3;

    // Used to revoke a credential batch.
    ProtoRevokeCredentials revoke_credentials = 4;

    // Used to announce new protocol update
    ProtoProtocolVersionUpdate protocol_version_update = 5;

    // Used to deactivate DID
    ProtoDeactivateDID deactivate_did = 6;

    // Used to create a public storage entry.
    ProtoCreateStorageEntry create_storage_entry = 7;

    // Used to update a storage entry.
    ProtoUpdateStorageEntry update_storage_entry = 8;

    // Used to deactivate a storage entry.
    ProtoDeactivateStorageEntry deactivate_storage_entry = 9;
  };
}

```

### File 'prism-version.proto'

```proto
// Specifies the protocol version update
message ProtoProtocolVersionUpdate {
  string proposer_did = 1; // The DID suffix that proposes the protocol update.
  ProtocolVersionInfo version = 2; // Information of the new version
}

message ProtocolVersion {
  // Represent the major version
  int32 major_version = 1;
  // Represent the minor version
  int32 minor_version = 2;
}

message ProtocolVersionInfo {
  reserved 2, 3;
  string version_name = 1; // (optional) name of the version
  int32 effective_since = 4; // Cardano block number that tells since which block the update is enforced

  // New major and minor version to be announced,
  // If major value changes, the node MUST stop issuing and reading events, and upgrade before `effective_since` because of the new protocol version.
  // If minor value changes, the node can opt not to update. All events _published_ by this node would also be
  // understood by other nodes with the same major version. However, there may be new events that this node won't _read_
  ProtocolVersion protocol_version = 5;
}
```

### File 'prism-ssi.proto'
```proto

// The event to create a public DID.
message ProtoCreateDID {
  DIDCreationData did_data = 1; // DIDCreationData with public keys and services

  // The data necessary to create a DID.
  message DIDCreationData {
    reserved 1; // Removed DID id field which is empty on creation
    repeated PublicKey public_keys = 2; // The keys that belong to this DID Document.
    repeated Service services = 3; // The list of services that belong to this DID Document.
    repeated string context = 4; // The list of @context values to consider on JSON-LD representations
  }
}

// Specifies the necessary data to update a public DID.
message ProtoUpdateDID {
  bytes previous_event_hash = 1; // The hash of the most recent event that was used to create or update the DID.
  string id = 2; // @exclude TODO: To be redefined after we start using this event.
  repeated UpdateDIDAction actions = 3; // The actual updates to perform on the DID.
}

message ProtoDeactivateDID {
  bytes previous_event_hash = 1; // The hash of the most recent event that was used to create or update the DID.
  string id = 2; // DID Suffix of the DID to be deactivated
}

// ##########

/**
 * Represents a public key with metadata, necessary for a DID document.
 */
message PublicKey {
  reserved 3, 4, 5, 6;
  string id = 1; // The key identifier within the DID Document.
  KeyUsage usage = 2; // The key's purpose.

  // The key's representation.
  oneof key_data {
    ECKeyData ec_key_data = 8; // The Elliptic Curve (EC) key.
    CompressedECKeyData compressed_ec_key_data =  9; // Compressed Elliptic Curve (EC) key.
  };
}

// Every key has a single purpose:
enum KeyUsage {
  // UNKNOWN_KEY is an invalid value - Protobuf uses 0 if no value is provided and we want the user to explicitly choose the usage.
  UNKNOWN_KEY = 0;
  MASTER_KEY = 1;
  ISSUING_KEY = 2;
  KEY_AGREEMENT_KEY = 3;
  AUTHENTICATION_KEY = 4;
  REVOCATION_KEY = 5;
  CAPABILITY_INVOCATION_KEY = 6;
  CAPABILITY_DELEGATION_KEY = 7;
  VDR_KEY = 8; // Create, Update, Remove - VDR entries. This key does not appear in the DID document.
}

/**
 * Holds the necessary data to recover an Elliptic Curve (EC)'s public key.
 */
 message ECKeyData {
  string curve = 1; // The curve name, like secp256k1.
  bytes x = 2; // The x coordinate, represented as bytes.
  bytes y = 3; // The y coordinate, represented as bytes.
}

/**
 * Holds the compressed representation of data needed to recover Elliptic Curve (EC)'s public key.
 */
message CompressedECKeyData {
  string curve = 1; // The curve name, like secp256k1.
  bytes data = 2; // compressed Elliptic Curve (EC) public key data.
}

message Service {
  string id = 1;
  string type = 2;
  string service_endpoint = 3;
}

// The potential details that can be updated in a DID.
message UpdateDIDAction {

  // The action to perform.
  oneof action {
    AddKeyAction add_key = 1; // Used to add a new key to the DID.
    RemoveKeyAction remove_key = 2; // Used to remove a key from the DID.
    AddServiceAction add_service = 3; // Used to add a new service to a DID,
    RemoveServiceAction remove_service = 4; // Used to remove an existing service from a DID,
    UpdateServiceAction update_service = 5; // Used to Update a list of service endpoints of a given service on a given DID.
    PatchContextAction patch_context = 6; // Used to Update a list of `@context` strings used during resolution for a given DID.
  }
}

// The necessary data to add a key to a DID.
message AddKeyAction {
  PublicKey key = 1; // The key to include.
}

// The necessary data to remove a key from a DID.
message RemoveKeyAction {
  string keyId = 1; // the key id to remove
}

message AddServiceAction {
  Service service = 1;
}

message RemoveServiceAction {
  string serviceId = 1;
}

message UpdateServiceAction {
  string serviceId = 1; // scoped to the did, unique per did
  string type = 2; // new type if provided
  string service_endpoints = 3;
}

message PatchContextAction {
  repeated string context = 1; // The list of strings to use by resolvers during resolution when producing a JSON-LD output
}

```

### File 'prism-storage.proto'

```proto

/* Notes:
 *
 * A Storage Event can be one of three types:
 *  - ProtoCreateStorageEntry
 *  - ProtoUpdateStorageEntry
 *  - ProtoDeactivateStorageEntry
 * Those three types/structures are independent. But at the same time (just for implementation convenience),
 *   they shared a common structure / field positions.
 *
 * The fields from positions 1 and 2 are reserved and used in the validation process of the Storage Events (currently).
 * The fields from positions 3 to 49 are reserved to be used in the validation process of the Storage Events (in the future).
 *   If one of those fields/position are present (in a valid event) the Indexer MUST consider the Storage Entry unsupported (not valid) from that moment, forward.
 * The fields from positions 50 to 99 are for adding relevant metadata that does not impact the validation process of the Storage Events.
 */

/** StorageEventCreateEntry
 * To be valid, this Event MUST be signed by an issuing key of the DID:
 *   1) The issuing key MUST be valid at the Event moment.
 *   2) The DID MUST not be Deactivated.
 */
message ProtoCreateStorageEntry {
  reserved 2; // These positions are reserved by ProtoUpdateStorageEntry & ProtoDeactivateStorageEntry
  reserved 3 to 49; // These fields will be used for validation the Storage Events in the future
  bytes did_prism_hash = 1; // The specificId of the did:prism.
  bytes nonce = 50; // Used to generate different reference hash (to make different entries with the same initial data possible)
  oneof data {
    // Nothing // The data field MAY be omitted representing ANY type.
    bytes bytes = 100;
    string ipfs = 101; // The string MUST be a CID.

    // ... future expansions
  }
}

/** StorageEventUpdateEntry
 * To be valid, this Event MUST be signed by an issuing key of the DID:
 * - 1) The issuing key MUST be valid at the Event moment.
 * - 2) The DID MUST not to be Deactivated.
 */
message ProtoUpdateStorageEntry {
  reserved 1, 50; // These positions are reserved by ProtoCreateStorageEntry
  reserved 3 to 49; // These fields will be used for validation the Storage Events in the future
  bytes previous_event_hash = 2; // The hash of the most recent event that was used to create or update the VDR Entry.
  oneof data {
    bytes bytes = 100; // Replace the bytes
    string ipfs = 101; // Update/replace the data with a CID to IPFS. The string MUST be a CID.

    // ... future expansions
  }
}

message ProtoDeactivateStorageEntry{
  reserved 1, 50; // These positions are reserved by ProtoCreateStorageEntry
  reserved 3 to 49; // These fields will be used for validation the Storage Events in the future
  bytes previous_event_hash = 2; // The hash of the most recent event that was used to create or update the VDR Entry.
}
```

### File 'prism-credential-batch'

```proto
// Represents a credential's batch.
// Specifies the data to issue a credential batch.
message ProtoIssueCredentialBatch {
  CredentialBatchData credential_batch_data = 1; // The actual credential batch data.
}
// Check the protocol docs to understand it.
message CredentialBatchData {
  string issuer_did = 1; // The DID suffix that issues the credential's batch.
  bytes merkle_root = 2; // The Merkle root for the credential's batch.
}
  
  
// Specifies the credentials to revoke (the whole batch, or just a subset of it).
message ProtoRevokeCredentials {
  bytes previous_event_hash = 1; // The hash of the event that issued the batch.
  string credential_batch_id = 2; // The corresponding batch ID, as returned in IssueCredentialBatchResponse.
  repeated bytes credentials_to_revoke = 3; // The hashes of the credentials to revoke. If empty, the full batch is revoked.
}
```


### Illustration of timeline chain Block/Transactions/Events/SSI/StorageEntry

This VDR follows the notion of Cardano time.
The Cardano blockchain has a chain of blocks of transactions. Each blocks was multiple transactions. Each transaction can have metadata with a PRISM Block. Each PRISM block can have a Sequence of Sign Events.
So all PRISM events has an order between them.

The image below illustrates how chains are formed in Cardano blocks, transactions, VDR events, SSI entries, and Storage entries.
<br> It also illustrates the relationship between chains and how everything is ordered in a timeline.

```mermaid
%%{init: { 'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchName': 'Cardano Blocks'}} }%%
gitGraph TB:
  commit id: "B1"

  branch "Transactions"
  checkout "Transactions"
  commit id: "B1-T1"

  branch "SignedPrismEvents"

  checkout "SignedPrismEvents"
  commit id: "B1-T1-O1"
  branch "SSI X"
  checkout "SSI X"

  commit id: "Create SSI"
  branch "Storage Entry 123"

  checkout SignedPrismEvents
  commit id: "B1-T1-O2"
  checkout "SSI X"
  merge "SignedPrismEvents" id:"Update Add Issuing Key"


  checkout "SignedPrismEvents"
  commit id: "B1-T1-O3"
  checkout "Storage Entry 123"
  merge "SignedPrismEvents" id: "Create Storage Entry 123"

  checkout "SignedPrismEvents"
  commit id: "B1-T1-O4"
  branch "SSI Y"
  checkout "SSI Y"
  commit id: "Create SSI Y"

  checkout "Transactions"
  commit id: "B1-T2"
  checkout "SignedPrismEvents"
  merge "Transactions" id:"B1-T2-O1"


  checkout "SSI Y"
  merge "SignedPrismEvents" id: "Update Keys"





  checkout "Cardano Blocks"
  commit id: "B2"

  checkout "Transactions"
  merge "Cardano Blocks" id: "B2-T1"
  
  checkout "SignedPrismEvents"
  merge "Transactions" id:"B2-T1-O1"

  checkout "SSI X"
  merge "SignedPrismEvents" id: "Update Rotate the Issuing keys"
 

  checkout "SignedPrismEvents"
  commit id: "B2-T1-O2"
  checkout "Storage Entry 123"
  merge "SignedPrismEvents" id: "Update Storage Entry 123"
```
