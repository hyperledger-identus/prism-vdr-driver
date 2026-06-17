package hyperledger.identus.vdr.prism

import fmgp.did.method.prism.RefVDR

/** Thrown when the path is invalid or the VDR entry identifier cannot be resolved. */
class DataCouldNotBeFoundException(reason: Option[String])
    extends Exception(
      "Could not find the data" + reason.map(s => s" because: $s").getOrElse("")
    )

/** Passive drivers do not support create, update, or delete operations. */
object UnsupportedNotPassiveMethodException
    extends RuntimeException("Passive driver does not support create/update/deactivate events")

/** Thrown by [[PRISMReadOnlyDriver.read]] when the on-chain entry has no payload yet (`DataEmpty`). */
case class DataNotInitializedException(ref: RefVDR)
    extends RuntimeException(s"'${ref.hex}' VDR entry was not initialized")

/** Thrown by [[PRISMReadOnlyDriver.read]] when the entry was deactivated. */
case class DataAlreadyDeactivatedException(ref: RefVDR)
    extends RuntimeException(s"'${ref.hex}' VDR entry is already deactivated")

/** Thrown when the entry type is not supported for the requested operation. */
case class DataOfUnexpectedTypeException(ref: RefVDR)
    extends RuntimeException(s"'${ref.hex}' VDR entry is of an unsupported type for this driver")
