package ch.epfl.bluebrain.nexus.workbench

import scala.util.control.NonFatal

import scala.util.{Failure, Success, Try}
import cats.instances.try_._
import sbt.io._

/**
  * Collects the schemas and/or contexts present in the classpath in a configured output folder,
  * by recursively replacing the baseUriToken by a configured baseUri.
  *
  */
object CollectResources {

  def main(args: Array[String]): Unit = {
    val list = args.toList
    list match {
      case baseUri :: baseUriToken :: collectTarget :: resourceType :: Nil =>
        apply(baseUri, baseUriToken, collectTarget, resourceType)
      case _ =>
        val msg = s"Illegal arguments: required '<baseUri> <baseUriToken> <collectTarget> <resourceType>', got '$list'"
        throw new IllegalArgumentException(msg)
    }
  }

  def apply(baseUri: String, baseUriToken: String, collectTarget: String, resourceType: String): Unit = {

    val resourceTypes = if (resourceType.equals("all")) List.apply("schemas", "contexts") else List.apply(resourceType)

    for (t <- resourceTypes) {
      println(s"Collect $t and stored them in $collectTarget")
      copyResourceTo(baseUri, baseUriToken, t, collectTarget)
    }

  }

  private def copyResourceTo(baseUri: String,
                             baseUriToken: String,
                             resourceType: String,
                             collectTarget: String): Unit = {
    import java.io.File
    import org.springframework.core.io.support.PathMatchingResourcePatternResolver
    import akka.http.scaladsl.model.Uri

    val loader   = new ResourceLoader[Try](Uri(baseUri), Map(baseUriToken -> baseUri))
    val cl       = getClass.getClassLoader
    val resolver = new PathMatchingResourcePatternResolver(cl)

    val resourceTypeDir       = "/" + resourceType + "/"
    val resources             = resolver.getResources("classpath*:" + resourceTypeDir + "*/*/*/*.json")
    val resourceTypeOutputDir = collectTarget + resourceTypeDir
    if (resources.nonEmpty) {
      val genDir = new File(resourceTypeOutputDir)
      genDir.mkdir
    }

    resources
      .map { res =>
        val uri      = Uri(res.getURI.toString)
        val str      = uri.toString()
        val stripped = str.substring(str.indexOf(resourceTypeDir) + resourceTypeDir.length, str.length - 5)
        (stripped, loader(Uri(s"$baseUri/$resourceType/$stripped"), loadCtx = false))
      }
      .foreach {
        case (stripped, Success(json)) =>
          IO.write(new File(s"$resourceTypeOutputDir/$stripped.json"), json.spaces2)
          println(s"Collected $resourceType '$stripped' to '$resourceTypeOutputDir/$stripped.json'")
        case (stripped, Failure(NonFatal(th))) =>
          throw new RuntimeException(s"Failed to export resource '$stripped'", th)
      }
  }
}
