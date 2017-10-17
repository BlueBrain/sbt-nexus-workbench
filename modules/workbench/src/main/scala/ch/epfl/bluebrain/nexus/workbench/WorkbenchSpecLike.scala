package ch.epfl.bluebrain.nexus.workbench

import java.io.File

import akka.http.scaladsl.model.Uri
import cats.instances.try_._
import ch.epfl.bluebrain.nexus.commons.shacl.validator.{ShaclSchema, ShaclValidator}
import org.scalatest.WordSpecLike
import sbt.io.syntax._

import scala.util.Try

trait WorkbenchSpecLike extends WordSpecLike with ValidationMatchers {

  def baseUri: String

  def baseUriToken: String

  def baseDir: String

  def testDir: String

  private val bd        = new File(baseDir).getAbsoluteFile
  private val td        = new File(testDir).getAbsoluteFile
  private val base      = Uri(baseUri)
  private val loader    = new ResourceLoader[Try](Uri(baseUri), Map(baseUriToken -> baseUri))
  private val resolver  = new ClasspathResolver[Try](loader)
  private val validator = ShaclValidator(resolver)

  private def schemas(): List[SchemaRef] = {
    val finder = bd * "schemas" * "*" * "*" * "*" * "*.json"
    finder.get.foldLeft(List.empty[SchemaRef]) {
      case (acc, e) =>
        val string = e.getAbsolutePath.substring(bd.getAbsolutePath.length + 1, e.getAbsolutePath.length - 5)
        val uri    = Uri(s"$base/$string")
        val ref    = SchemaRef(base, uri)
        ref :: acc
    }
  }

  private def valid(schemaRef: SchemaRef): List[InstanceRef] = {
    val finder = td * "data" * "*" * "*" * "*" * "*" * "*.json"
    finder.get.foldLeft(List.empty[InstanceRef]) {
      case (acc, e) =>
        val string = e.getAbsolutePath.substring(td.getAbsolutePath.length + 1, e.getAbsolutePath.length - 5)
        val uri    = Uri(s"$base/$string")
        val ref    = InstanceRef(base, uri)
        if (ref.stripped.startsWith(s"/data/${schemaRef.stripped}")) ref :: acc
        else acc
    }
  }

  private def invalid(schemaRef: SchemaRef): List[InstanceRef] = {
    val finder = td * "invalid" * "*" * "*" * "*" * "*" * "*.json"
    finder.get.foldLeft(List.empty[InstanceRef]) {
      case (acc, e) =>
        val string = e.getAbsolutePath.substring(td.getAbsolutePath.length + 1, e.getAbsolutePath.length - 5)
        val uri    = Uri(s"$base/$string")
        val ref    = InstanceRef(base, uri)
        if (ref.stripped.startsWith(s"/invalid/${schemaRef.stripped}")) ref :: acc
        else acc
    }
  }

  schemas().foreach { implicit schemaRef =>
    s"The '${schemaRef.stripped}' schema" should {

      "be loaded correctly in the validator" in {
        ShaclSchema(loader(schemaRef.uri).value)
      }

      val validInstances = valid(schemaRef)
      if (validInstances.nonEmpty) {
        "validate when applied to an instance" when {
          val schema = ShaclSchema(loader(schemaRef.uri).value)
          validInstances.foreach { implicit ref =>
            if (ref.isIgnored) {
              s"using '${ref.stripped}'" ignore {
                val instance = loader(ref.uri).value
                validator(schema, instance).shouldConform
              }
            } else {
              s"using '${ref.stripped}'" in {
                val instance = loader(ref.uri).value
                validator(schema, instance).shouldConform
              }
            }
          }
        }
      }

      val invalidInstances = invalid(schemaRef)
      if (invalidInstances.nonEmpty) {
        "NOT validate when applied to an instance" when {
          val schema = ShaclSchema(loader(schemaRef.uri).value)
          invalidInstances.foreach { implicit ref =>
            if (ref.isIgnored) {
              s"using '${ref.stripped}'" ignore {
                val instance = loader(ref.uri).value
                validator(schema, instance).shouldNotConform
              }
            } else {
              s"using '${ref.stripped}'" in {
                val instance = loader(ref.uri).value
                validator(schema, instance).shouldNotConform
              }
            }
          }
        }
      }
    }
  }
}
