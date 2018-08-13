package ch.epfl.bluebrain.nexus.workbench

import java.io.File

import akka.http.scaladsl.model.Uri
import cats.instances.try_._
import org.scalatest._
import sbt.io.syntax._

import scala.util.Try

trait WorkbenchSpecLike extends WordSpecLike with ValidationMatchers with BeforeAndAfterAllConfigMap {

  def baseUri: String

  def baseUriToken: String

  def baseDir: String

  def testDir: String

  private val bd                   = new File(baseDir).getAbsoluteFile
  private val td                   = new File(testDir).getAbsoluteFile
  private val base                 = Uri(baseUri)
  private val loader               = new ResourceLoader[Try](Uri(baseUri), Map(baseUriToken -> baseUri))
  private val resolver             = new ClasspathResolver[Try](loader)
  private val validator            = ShaclValidator(resolver)
  private var ignoreShacl: Boolean = false

  override def beforeAll(configMap: ConfigMap) = {
    ignoreShacl = configMap.getWithDefault("ignoreShacl", "false").toBoolean
  }

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

      "be validated correctly against the SHACL schema" in {
        assume(!ignoreShacl)
        val schema = ShaclSchema(loader(schemaRef.uri).value)
        validator(schema).shouldConform
      }

      val validInstances = valid(schemaRef)
      if (validInstances.nonEmpty) {
        "validate when applied to an instance" when {
          val schema = ShaclSchema(loader(schemaRef.uri).value)
          validInstances.foreach { ref =>
            implicit val refOpt: Option[InstanceRef] = Some(ref)
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
            implicit val refOpt: Option[InstanceRef] = Some(ref)
            if (ref.isIgnored) {
              s"using '${ref.stripped}'" ignore {
                val instance = loader(ref.uri).value
                validator(schema).shouldConform
                validator(schema, instance).shouldNotConform
              }
            } else {
              s"using '${ref.stripped}'" in {
                val instance = loader(ref.uri).value
                validator(schema).shouldConform
                validator(schema, instance).shouldNotConform
              }
            }
          }
        }
      }
    }
  }
}
