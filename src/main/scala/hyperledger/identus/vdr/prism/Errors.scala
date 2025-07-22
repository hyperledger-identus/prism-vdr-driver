package hyperledger.identus.vdr.prism

class DataCouldNotBeFoundException(reason: Option[String])
    extends Exception(
      "Could not find the data" + reason.map(s => s" becuase: $s").getOrElse("")
    )
class DataNotInitializedException extends Exception("Data wasn't initialized")
class DataAlreadyDeactivatedException extends Exception("Data was deactivated")
//class InvalidRequest(reason: String) extends Exception(s"Invalid request because: $reason")
