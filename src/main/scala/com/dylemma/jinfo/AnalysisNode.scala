package com.dylemma.jinfo
import org.neo4j.graphdb.{ Node, Direction }
import scala.collection.JavaConversions._
import org.neo4j.graphdb.RelationshipType
import scalax.file.Path
import org.neo4j.graphdb.GraphDatabaseService
import japa.parser.ast.PackageDeclaration

case class AnalysisNode(node: Node) {
	lazy val graph = node.getGraphDatabase

	def defaultPackage = hierarchicalRoot(PackageHierarchy)(_.setProperty("name", ""))
	def defaultFile = hierarchicalRoot(FileHierarchy)(_.setProperty("name", ""))

	def hierarchicalRoot(relType: RelationshipType)(init: Node => Unit): Node =
		node.getSingleRelationship(relType, Direction.OUTGOING) match {
			case null =>
				val r = node.getGraphDatabase.createNode
				node.createRelationshipTo(r, relType)
				init(r)
				r
			case edge => edge.getEndNode
		}

	/** Accesses the node associated with the package, as specified by `fullName`.
	  * If `createIfMissing` is set to `true`, any missing nodes will be created,
	  * and this method will always return a `Some`.
	  * @param fullName the full name of the package, e.g. `com.foo.bar`
	  * @param createIfMissing flag, described above. Defaults to `true` If the
	  * node needs to be created, the underlying graph must be in a transaction
	  * @return An option containing the described node.
	  */
	def pkg(fullName: String, createIfMissing: Boolean = true) = {
		val nameParts = if (fullName.isEmpty) Nil else fullName.split("\\.").toList
		nodeFromPath(defaultPackage, nameParts, PackageHierarchy, createIfMissing)(name => graph.createNode)
	}

	def typeNode(name: String, pkgDec: PackageDeclaration, createIfMissing: Boolean): Option[Node] = {
		typeNode(name, Option(pkgDec).map(_.getName.toString).getOrElse(""), createIfMissing)
	}

	def typeNode(name: String, pkgName: String, createIfMissing: Boolean = true): Option[Node] = {
		val pkgNode = pkg(pkgName, createIfMissing)
		pkgNode flatMap { p =>
			nodeFromPath(p, List(name), PackageToClass, createIfMissing)(name => graph.createNode)
		}
	}

	def file(path: Path, createIfMissing: Boolean = true) = {
		nodeFromPath(defaultFile, path.segments.toList, FileHierarchy, createIfMissing)(name => graph.createNode)
	}

	def nodeFromPath(start: Node, path: List[String], relType: RelationshipType, createIfMissing: Boolean = true,
		direction: Direction = Direction.OUTGOING,
		nameProp: String = "name")(
			create: String => Node) = {

		def followPath(from: Node, p: List[String]): Option[Node] = p match {
			case Nil => Some(from)
			case head :: tail =>
				val nextNode = from.getRelationships(relType, direction).find {
					case edge => edge.getEndNode.getProperty(nameProp) == head
				}
				nextNode match {
					case None if !createIfMissing => None
					case Some(edge) => followPath(edge.getEndNode, tail)
					case None =>
						val next = create(head)
						next.setProperty(nameProp, head)
						from.createRelationshipTo(next, relType)
						followPath(next, tail)
				}
		}

		followPath(start, path)
	}

}

object AnalysisNode {

	def getFrom(db: DB) = db.withoutTransaction { graph =>
		lookupAnalysisNode(graph, false)
	}
	def fromDB(db: DB) = db.withTransaction { graph =>
		lookupAnalysisNode(graph, true).getOrElse {
			//TODO: a different type of exception would be good
			throw new RuntimeException("Unable to lookup/create an AnalysisNode")
		}
	}

	private def lookupAnalysisNode(graph: GraphDatabaseService, create: Boolean) = {
		val refNode = graph.getReferenceNode
		refNode.getSingleRelationship(RefToAnalysis, Direction.OUTGOING) match {
			case null if !create => None
			case null =>
				val node = graph.createNode
				refNode.createRelationshipTo(node, RefToAnalysis)
				node.setProperty("kind", "AnalysisNode")
				Some(AnalysisNode(node))
			case edge => Some(AnalysisNode(edge.getEndNode))
		}
	}
}