package ch.epfl.bluebrain.nexus.workbench

/**
  * Top level error type for the sealed hierarchy of shacl validation errors.
  *
  * @param message a text describing the reason as to why this exception has been raised
  * @param fatal flag to signal whether the error is fatal or not. Fatal errors will throw an exception
  *              while checking for validation
  */
@SuppressWarnings(Array("IncorrectlyNamedExceptions"))
sealed abstract class ShaclValidatorErr(message: String, val fatal: Boolean) extends Err(message)

@SuppressWarnings(Array("IncorrectlyNamedExceptions"))
object ShaclValidatorErr {

  /**
    * An error that describes the failure to load a shacl schema into the validator.
    *
    * @param cause the underlying cause for this exception
    */
  final case class FailedToLoadShaclSchema(cause: Throwable)
      extends ShaclValidatorErr("Failed to load a shacl schema", false)

  /**
    * An error that describes the failure to load data into the validator.
    *
    * @param message the underlying error message for this exception
    */
  final case class FailedToLoadData(override val message: String) extends ShaclValidatorErr(message, false)

  /**
    * An error that describes the failure to generate the report.
    */
  final case object FailedToGenerateReport extends ShaclValidatorErr("failed to generate report", false)

  /**
    * An error that describes a failed attempt to load referenced schemas.
    *
    * @param missing the schema addresses that could not be loaded
    */
  final case class CouldNotFindImports(missing: Set[String])
      extends ShaclValidatorErr("Failed to load referenced schemas", true)

  /**
    * An error that describes a failure to follow a schema import.
    *
    * @param values the illegal imports
    */
  final case class IllegalImportDefinition(values: Set[String])
      extends ShaclValidatorErr("Failed to follow schema imports, illegal definition", true)

  /**
    * An error that describes that the schema file was not found.
    *
    * @param message the underlying error message for this exception
    */
  final case class FileNotFound(override val message: String) extends ShaclValidatorErr(message, true)

}

/**
  * Top level error type that does not fill in the stack trace when thrown.  It also enforces the presence of a message.
  *
  * @param message a text describing the reason as to why this exception has been raised.
  */
@SuppressWarnings(Array("IncorrectlyNamedExceptions"))
abstract class Err(val message: String) extends Exception {
  override def fillInStackTrace(): Err = this
  override val getMessage: String      = message
}
