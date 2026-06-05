# PRISM VDR Driver

## PRISM

This driver implements the [Generic VDR specification][Generic-VDR] following the [PRISM VDR Protocol][PRISM-VDR] that uses the Cardano blockchain as the source of truth.

The driver following the [PRISM VDR Protocol][PRISM-VDR] to encode data on the blockchain.
It's based on the [DID PRISM Specification][DID-PRISM].

## Project Catalyst

**Verifiable Data Registry for Identus in Cardano**

This Driver fulfill one of the acceptance criteria in **Milestone 3** of Project `1300189` https://milestones.projectcatalyst.io/projects/1300189/milestones/3 (Delivery 8 Sep 2025).

## Documentation

Learn more:

- [PRISM VDR specification][PRISM-VDR]
- [Generic VDR specification][Generic-VDR]

### Read / update / delete errors

`read`, `update`, and `delete` signal failures with exceptions (documented in the
[generic VDR `Driver` contract](https://github.com/hyperledger-identus/vdr/blob/main/src/main/kotlin/org/hyperledger/identus/vdr/interfaces/DriverExceptions.kt)):

| Exception | Meaning |
| --- | --- |
| `DataCouldNotBeFoundException` | Missing path identifier or unknown entry |
| `DataNotInitializedException` | Entry exists on-chain but has no payload yet |
| `DataAlreadyDeactivatedException` | Entry was deactivated |
| `DataOfUnexpectedTypeException` | Payload type unsupported for this operation |

`PRISMReadOnlyDriver.read` throws `DataNotInitializedException` or `DataAlreadyDeactivatedException`
instead of returning an empty byte array, so HTTP layers can return distinct status codes.
See [hyperledger-identus/vdr#23](https://github.com/hyperledger-identus/vdr/issues/23) and
[hyperledger-identus/prism-vdr-driver#41](https://github.com/hyperledger-identus/prism-vdr-driver/issues/41).

## Versions

All published JVM versions are available in [Maven Central - Sonatype](https://central.sonatype.com/artifact/org.hyperledger.identus/prism-vdr-driver_3/versions).

## Contributing

For the general guidelines, see Hyperledger [contributor's guide](https://github.com/hyperledger-identus/hyperledger-identus/blob/main/CONTRIBUTING.md).

## Code of Conduct

See the [Code of Conduct](https://github.com/hyperledger-identus/hyperledger-identus/blob/main/CODE_OF_CONDUCT.md).

## Support

Come chat with us on [![Badge-Discord]][Link-Discord].

## License

[License](LICENSE)

[PRISM-VDR]: ./prism-vdr-specification.md "PRISM VDR specification"
[Generic-VDR]: https://github.com/hyperledger-identus/vdr "Generic VDR specification - API / Interface"
[DID-PRISM]: https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md "DID PRISM Specification"
[Badge-Discord]: https://img.shields.io/discord/629491597070827530?logo=discord "LFDT - Linux Foundation Decentralized Trust"
[Link-Discord]: https://discord.gg/hyperledger "Discord"
