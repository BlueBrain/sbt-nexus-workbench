package ch.epfl.bluebrain.nexus.workbench

import ch.epfl.bluebrain.nexus.rdf.Iri.AbsoluteIri
import ch.epfl.bluebrain.nexus.rdf.Vocabulary._
import ch.epfl.bluebrain.nexus.workbench.TripleProcessing._
import org.apache.jena.datatypes.TypeMapper
import org.apache.jena.datatypes.xsd.XSDDatatype._
import org.apache.jena.graph.{Graph, Node, Triple}
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.system.{StreamRDFLib, StreamRDFWrapper}

import scala.util.Try

/**
  * Extends the default ''StreamRDFLib'' stream in order to convert the literals
  * that match ''xsd:dateTime'' or ''xsd:date'' or ''xsd:time'' to the corresponding triples
  * @param graph the model graph
  */
class TripleProcessing(graph: Graph) extends StreamRDFWrapper(StreamRDFLib.graph(graph)) {

  override def triple(triple: Triple): Unit = {
    val modObject = asStringLabel(triple.getObject)
      .flatMap(string => cast(string, xsd.dateTime) orElse cast(string, xsd.date) orElse cast(string, xsd.time))
      .getOrElse(triple.getObject)
    super.triple(new Triple(triple.getSubject, triple.getPredicate, modObject))
  }
}

object TripleProcessing {

  private[workbench] def cast(lexicalText: String, dataType: AbsoluteIri): Option[Node] =
    Try {
      val tpe     = TypeMapper.getInstance().getSafeTypeByName(dataType.asString)
      val literal = ResourceFactory.createTypedLiteral(lexicalText, tpe)
      literal.getValue //It will crash whenever the literal does not match the desired datatype
      literal.asNode()
    }.toOption

  /**
    * @return a new [[TripleProcessing]] from a provided graph
    */
  final def apply(graph: Graph): TripleProcessing = new TripleProcessing(graph)

  private[workbench] def asStringLabel(n: Node): Option[String] =
    Option(
      n.isLiteral && (n.getLiteralLanguage == null || n.getLiteralLanguage.isEmpty) &&
        (n.getLiteralDatatype == null || n.getLiteralDatatype == XSDstring)).collect {
      case true => n.getLiteral.getLexicalForm
    }
}
