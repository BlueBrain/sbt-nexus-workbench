package ch.epfl.bluebrain.nexus.workbench

import akka.http.scaladsl.model.Uri

final case class SchemaRef(base: Uri, uri: Uri) {

  lazy val stripped: String =
    uri.toString().substring(s"$base/schemas/".length)

}
