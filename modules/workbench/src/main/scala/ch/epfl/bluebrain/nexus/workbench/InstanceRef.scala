package ch.epfl.bluebrain.nexus.workbench

import akka.http.scaladsl.model.Uri

final case class InstanceRef(base: Uri, uri: Uri) {

  lazy val stripped: String =
    uri.toString().substring(base.toString().length)

  lazy val isIgnored: Boolean =
    uri.toString().endsWith("-ignored")
}
