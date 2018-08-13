package ch.epfl.bluebrain.nexus.workbench

import ch.epfl.bluebrain.nexus.commons.shacl.topquadrant.ValidationReport
import io.circe.Json
import org.scalactic.source
import org.scalatest._
import org.scalatest.exceptions.{StackDepthException, TestFailedException}

import scala.util.{Failure, Success, Try}

trait ValidationMatchers extends Matchers { self: WordSpecLike =>

  def validate: AfterWord    = afterWord("validate instances")
  def notValidate: AfterWord = afterWord("NOT validate instances")

  implicit def conformantOrNotFromTry(vtry: Try[ValidationReport])(
      implicit
      pos: source.Position,
      schemaRef: SchemaRef,
      instanceRef: Option[InstanceRef] = None): ConformantOrNot = {
    vtry match {
      case Success(report) => new ConformantOrNot(report)
      case Failure(e: ShaclValidatorErr) if !e.fatal =>
        new ConformantOrNot(new ValidationReport(false, 0, Json.fromString(e.message)))
      case Failure(e: ShaclValidatorErr) =>
        throw new TestFailedException((_: exceptions.StackDepthException) => Some(e.message), Some(e.getCause), pos)
      case Failure(th) =>
        val message = "Unexpected failure of the Shacl validator"
        throw new TestFailedException((_: exceptions.StackDepthException) => Some(message), Some(th), pos)
    }
  }

  class ConformantOrNot(report: ValidationReport)(implicit pos: source.Position,
                                                  schemaRef: SchemaRef,
                                                  instanceRef: Option[InstanceRef]) {
    def shouldConform: Assertion =
      if (report.isValid()) Succeeded
      else if (!report.conforms)
        throw new TestFailedException((_: exceptions.StackDepthException) => Some(report.json.spaces4), None, pos)
      else
        throw new TestFailedException(
          (_: exceptions.StackDepthException) => Some("No shapes were selected for validation"),
          None,
          pos)

    def shouldNotConform: Assertion =
      if (report.isValid()) {
        val message = instanceRef match {
          case Some(i) => s"Instance '${i.stripped}' conformed to schema '${schemaRef.stripped}'"
          case None    => s"Schema '${schemaRef.stripped}' conformed with SHACL schema"
        }
        throw new TestFailedException((_: exceptions.StackDepthException) => Some(message), None, pos)
      } else Succeeded
  }

  implicit def loadedOrNotFromTry(ltry: Try[Json])(implicit pos: source.Position): LoadedOrNot =
    new LoadedOrNot(ltry)

  class LoadedOrNot(ltry: Try[Json])(implicit pos: source.Position) {
    def value: Json = ltry match {
      case Success(json) => json
      case Failure(wbe: WorkbenchErr) =>
        val message = wbe.message
        throw new TestFailedException((_: exceptions.StackDepthException) => Some(message), Some(wbe), pos)
      case Failure(th) =>
        val message = "Resource loading failed unexpectedly"
        throw new TestFailedException((_: StackDepthException) => Some(message), Some(th), pos)
    }
  }

}
