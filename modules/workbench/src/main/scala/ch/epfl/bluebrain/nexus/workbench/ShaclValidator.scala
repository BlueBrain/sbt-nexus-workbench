package ch.epfl.bluebrain.nexus.workbench
import cats.MonadError
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.semigroupal._
import ch.epfl.bluebrain.nexus.commons.shacl.topquadrant.{ShaclEngine, ValidationReport}
import ch.epfl.bluebrain.nexus.workbench.ShaclValidatorErr._
import io.circe.Json
import journal.Logger
import org.apache.jena.rdf.model._
import org.apache.jena.riot.system.StreamRDFLib
import org.apache.jena.riot.{Lang, RDFParser}

import scala.util._
import scala.util.control.NonFatal

/**
  * ShaclValidator implementation based on ''org.topbraid.shacl'' validator.  It does not impose the use of a particular
  * effect handling implementation.
  *
  * @param importResolver a transitive import resolver for schemas
  * @param F              an implicitly available MonadError typeclass for ''F[_]''
  * @tparam F the monadic effect type
  */
final class ShaclValidator[F[_]](importResolver: ImportResolver[F])(implicit F: MonadError[F, Throwable]) {

  private val logger = Logger[this.type]

  /**
    * Validates ''data'' in its json representation against the specified ''schema''.  It produces a
    * ''ValidationReport'' in the ''F[_]'' context.
    *
    * @param schema         the shacl schema instance against which data is validated
    * @param data           the data to be validated
    * @return a ''ValidationReport'' in the ''F[_]'' context
    */
  def apply(schema: ShaclSchema, data: Json): F[ValidationReport] =
    loadData(data) product loadSchema(schema.value) flatMap {
      case (mod, sch) => validate(mod, sch, validateShapes = false)
    } recoverWith {
      case err =>
        logError(err)
        F.raiseError(err)
    }

  /**
    * Validates the argument ''schema'' against its specification.  It produces a ''ValidationReport'' in the ''F[_]''
    * context.
    *
    * @param schema the schema instance to be validated
    * @return a ''ValidationReport'' in the ''F[_]'' context
    */
  def apply(schema: ShaclSchema): F[ValidationReport] =
    loadSchema(schema.value)
      .flatMap[ValidationReport] { m =>
        ShaclEngine(m, reportDetails = true) match {
          case Some(r) => F.pure(r)
          case None    => F.raiseError(FailedToGenerateReport)
        }
      }
      .recoverWith {
        case err =>
          logError(err)
          F.raiseError(err)
      }

  private def loadSchema(schema: Json): F[Model] = {
    logger.debug("Loading schema for validation")
    importResolver(ShaclSchema(schema)).flatMap { set =>
      Try {
        logger.debug(s"Loaded '${set.size}' imports, aggregating shapes")
        val m = ModelFactory.createDefaultModel()
        (set + ShaclSchema(schema)).foreach(e => model(m, e.value))
        m
      } match {
        case Success(value) =>
          logger.debug("Schema loaded successfully")
          F.pure(value)
        case Failure(err @ NonFatal(th)) =>
          logError(err)
          F.raiseError(FailedToLoadShaclSchema(th))
        case Failure(err) =>
          logError(err)
          F.raiseError(err)
      }
    }
  }

  private def logError(err: Throwable): Unit = {
    err match {
      case err: CouldNotFindImports =>
        logger.debug(s"The provided imports '${err.missing}' are missing")
        F.raiseError(err)
      case err: IllegalImportDefinition =>
        logger.debug(s"The provided imports '${err.values}' are invalid")
      case err: FailedToLoadData =>
        logger.debug(s"Failed to load data into validtor '${err.message}'")
      case ve: ShaclValidatorErr =>
        logger.debug(s"Validation error '${ve.getMessage}'")
      case NonFatal(th) =>
        logger.debug(s"Validation error '${th.getMessage}' ")
    }
  }
  private def loadData(data: Json): F[Model] = {
    logger.debug("Loading data for validation")
    processedModel(data) match {
      case Right(value) =>
        logger.debug("Data loaded successfully")
        F.pure(value)
      // $COVERAGE-OFF$
      case Left(message) =>
        logger.debug(s"Failed to load schema '${data.spaces4}' for validation")
        F.raiseError(FailedToLoadData(message))
      // $COVERAGE-ON$
    }
  }

  private def validate(data: Model, schema: Model, validateShapes: Boolean): F[ValidationReport] = {
    logger.debug("Validating data against schema")
    ShaclEngine(data, schema, validateShapes, reportDetails = true) match {
      case Some(r) => F.pure(r)
      case None    => F.raiseError(e = FailedToGenerateReport)
    }
  }

  private def processedModel(json: Json): Either[String, Model] =
    Try(model(ModelFactory.createDefaultModel, json, processing = true))
      .fold(e => Left(s"Exception: ${e.getMessage}"), Right.apply)

  private def model(m: Model, json: Json, processing: Boolean = false): Model = {
    val stream = if (processing) TripleProcessing(m.getGraph) else StreamRDFLib.graph(m.getGraph)
    RDFParser.create.fromString(json.noSpaces).base("").lang(Lang.JSONLD).parse(stream)
    m
  }
}

object ShaclValidator {

  /**
    * Constructs a new ''ShaclValidator'' instance with an ''F[_]'' context using an implicitly available ''MonadError''
    * typeclass for ''F[_]''.
    *
    * @param importResolver a transitive import resolver for schemas
    * @param F              an implicitly available MonadError typeclass for ''F[_]''
    * @tparam F the monadic effect type
    * @return a new ''ShaclValidator'' instance with an ''F[_]'' context
    */
  final def apply[F[_]](importResolver: ImportResolver[F])(implicit F: MonadError[F, Throwable]): ShaclValidator[F] =
    new ShaclValidator[F](importResolver)
}
