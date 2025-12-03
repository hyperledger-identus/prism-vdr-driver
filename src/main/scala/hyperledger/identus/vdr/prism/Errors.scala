package hyperledger.identus.vdr.prism

import fmgp.did.method.prism.RefVDR

class DataCouldNotBeFoundException(reason: Option[String])
    extends Exception(
      "Could not find the data" + reason.map(s => s" becuase: $s").getOrElse("")
    )
object UnsupportedNotPassiveMethodException
    extends RuntimeException("Passive driver does not support create/update/deactivate events")

case class DataNotInitializedException(ref: RefVDR)
    extends RuntimeException(s"'${ref.hex}' VDR entry was not initialized")

case class DataAlreadyDeactivatedException(ref: RefVDR)
    extends RuntimeException(s"'${ref.hex}' VDR entry is already deactivated")

case class DataOfUnexpectedTypeException(ref: RefVDR)
    extends RuntimeException(s"'${ref.hex}' VDR entry is of an unsupported type for this driver")
