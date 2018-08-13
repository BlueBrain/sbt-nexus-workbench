package ch.epfl.bluebrain.nexus.workbench

import java.io.File

import akka.http.scaladsl.model.Uri
import cats.instances.try_._
import sbt.io._
import sbt.io.syntax._

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Exports the collection of schemas in a configured output folder, by recursively processing the contexts such that
  * each exported schema has a self contained context.
  */
// $COVERAGE-OFF$
object SchemaExport {

  def main(args: Array[String]): Unit = {
    val list = args.toList
    list match {
      case baseUri :: baseUriToken :: baseDir :: exportDir :: Nil =>
        apply(baseUri, baseUriToken, baseDir, exportDir)
      case _ =>
        val msg = s"Illegal arguments: required '<baseUri> <baseUriToken>, <baseDir>, <exportDir>', got '$list'"
        throw new IllegalArgumentException(msg)
    }
  }

  def apply(baseUri: String, baseUriToken: String, baseDir: String, exportDir: String): Unit = {
    val bd     = new File(baseDir).getAbsoluteFile
    val ed     = new File(exportDir).getAbsoluteFile
    val base   = Uri(baseUri)
    val loader = new ResourceLoader[Try](Uri(baseUri), Map(baseUriToken -> baseUri))

    println(s"Export base uri: $base")
    println(s"Export location: ${ed.getAbsolutePath}")

    schemas(bd, base)
      .map(ref => (ref, loader(ref.uri)))
      .foreach {
        case (ref, Success(json)) =>
          IO.write(new File(s"$ed/${ref.stripped}.json"), json.spaces2)
          println(s"Exported '${ref.stripped}'")
        case (ref, Failure(NonFatal(th))) =>
          throw new RuntimeException(s"Failed to export schema '${ref.stripped}'", th)
      }
  }

  private def schemas(bd: File, base: Uri): List[SchemaRef] = {
    val finder = bd * "schemas" * "*" * "*" * "*" * "*.json"
    finder.get.foldLeft(List.empty[SchemaRef]) {
      case (acc, e) =>
        val string = e.getAbsolutePath.substring(bd.getAbsolutePath.length + 1, e.getAbsolutePath.length - 5)
        val uri    = Uri(s"$base/$string")
        val ref    = SchemaRef(base, uri)
        ref :: acc
    }
  }

}
// $COVERAGE-ON$
