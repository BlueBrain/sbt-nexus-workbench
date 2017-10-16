package ch.epfl.bluebrain.nexus.workbench

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import ch.epfl.bluebrain.nexus.commons.types.Err
import io.circe.Json

/**
  * Sealed hierarchy of errors specific to the workbench.
  *
  * @param reason the reason why the error occurred
  */
@SuppressWarnings(Array("IncorrectlyNamedExceptions"))
sealed abstract class WorkbenchErr(reason: String) extends Err(reason)

@SuppressWarnings(Array("IncorrectlyNamedExceptions"))
object WorkbenchErr {

  /**
    * Signals the use of a relative uri.
    *
    * @param uri the offending uri
    */
  final case class NonAbsoluteUri(uri: Uri) extends WorkbenchErr(s"Cannot load non absolute uri '$uri'.")

  /**
    * Signals the occurrence of a file that it's not a syntactically correct JSON.
    *
    * @param address the address of the offending file
    */
  final case class InvalidJson(address: Path)
      extends WorkbenchErr(s"Cannot parse resource '$address' to a json format.")

  /**
    * Signals the inability to load a resource.
    *
    * @param address the address of the referenced resource that failed to load
    */
  final case class UnableToLoad(address: Path)
      extends WorkbenchErr(s"Unable to load resource '$address', validate it exists and that is readable.")

  /**
    * Signals the incorrect use of reference uri.
    *
    * @param address the address of the resource
    * @param base    the expected base uri
    */
  final case class IllegalResourceAddress(address: Uri, base: Uri)
      extends WorkbenchErr(s"Illegal resource address '$address', must start with '$base'.")

  /**
    * Signals the incorrect value of a context definition.
    *
    * @param ctx the offending context value
    */
  final case class IllegalContextValue(ctx: Json) extends WorkbenchErr(s"Illegal context definition: '${ctx.spaces2}'")

}
