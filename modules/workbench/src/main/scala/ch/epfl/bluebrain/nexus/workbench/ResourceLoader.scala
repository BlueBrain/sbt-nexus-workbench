package ch.epfl.bluebrain.nexus.workbench

import java.util.regex.Pattern

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import cats.MonadError
import cats.implicits._
import ch.epfl.bluebrain.nexus.workbench.WorkbenchErr._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Json, JsonObject, ParsingFailure}

import scala.annotation.tailrec
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Recursively loads json resources from the class path and replaces tokens in the contents.
  *
  * @param base         the base uri that should equate the working dir
  * @param replacements the collection of replacements to be performed on the loaded resources
  */
class ResourceLoader[F[_]](base: Uri, replacements: Map[String, String])(implicit F: MonadError[F, Throwable]) {
  require(base.isAbsolute, "The resource loader requires an absolute 'base' uri.")

  /**
    * Attempts to load the argument resource from the classpath and parse it as a JSON object.
    *
    * @param resource the resource uri
    */
  def apply(resource: Uri): F[Json] =
    if (!resource.isAbsolute) F.raiseError(NonAbsoluteUri(resource))
    else load(resource)

  private[workbench] final val baseUri: Uri =
    base.copy(path = stripTrailingSlashes(base.path))

  private val (baseAsString, length) = {
    val str = baseUri.toString()
    (str, str.length)
  }

  private def resolve(resource: Uri): F[Path] = {
    val str = resource.toString()
    if (!str.startsWith(baseAsString)) F.raiseError(IllegalResourceAddress(resource, base))
    else {
      val remainder = str.substring(length)
      Try(Path(s"$remainder.json")) match {
        case Failure(NonFatal(_)) => F.raiseError(IllegalResourceAddress(resource, base))
        case Success(path)        => F.pure(path)
      }
    }
  }

  private def load(resource: Uri): F[Json] = {
    resolve(resource).flatMap { address =>
      val replaced = Try {
        val asString = Source.fromInputStream(getClass.getResourceAsStream(address.toString())).mkString
        replacements.foldLeft(asString) {
          case (acc, (token, replacement)) =>
            acc.replaceAll(Pattern.quote(token), replacement)
        }
      }.toEither
      replaced.flatMap(str => parse(str)) match {
        case Left(_: ParsingFailure) => F.raiseError(InvalidJson(address))
        case Left(NonFatal(_))       => F.raiseError(UnableToLoad(address))
        case Right(json)             => loadContext(json)
      }
    }
  }

  private def loadContext(json: Json): F[Json] = {
    def handleContextObj(ctx: Json): F[Json] = {
      (ctx.asString, ctx.asObject, ctx.asArray) match {
        case (Some(str), _, _) =>
          load(Uri(str)).flatMap(_.hcursor.get[Json]("@context").map(handleContextObj).getOrElse(F.pure(Json.obj())))
        case (_, Some(_), _) =>
          F.pure(ctx)
        case (_, _, Some(arr)) =>
          F.sequence(arr.map(handleContextObj))
            .map(values =>
              values.foldLeft(Json.obj()) { (acc, e) =>
                acc deepMerge e
            })
        case _ => F.raiseError(IllegalContextValue(ctx))
      }
    }

    def inner(jsonObj: JsonObject): F[JsonObject] =
      F.sequence(jsonObj.toVector.map {
          case ("@context", v) => handleContextObj(v).map("@context" -> _)
          case (k, v)          => loadContext(v).map(k               -> _)
        })
        .map(JsonObject.fromIterable)

    json.arrayOrObject[F[Json]](F.pure(json),
                                arr => F.sequence(arr.map(loadContext)).map(Json.fromValues),
                                obj => inner(obj).map(_.asJson))
  }

  private def stripTrailingSlashes(path: Path): Path = {
    @tailrec
    def strip(p: Path): Path = p match {
      case Path.Empty       => Path.Empty
      case Path.Slash(rest) => strip(rest)
      case other            => other
    }

    strip(path.reverse).reverse
  }
}
