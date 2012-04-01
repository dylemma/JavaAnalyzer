package com.dylemma.jinfo

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Node
import scala.collection.JavaConversions._
import scalax.file.Path
import org.neo4j.graphdb.RelationshipType

object DBTest extends App {

	val db = DB("db/dbtest")
	val geotoolsRoot = Path("test-inputs", "geotools", "modules", "library")
	val geotoolsSmallSubset = geotoolsRoot / "api" / "src" / "main" / "java" / "org" / "geotools" / "filter" / "expression"

	val analysisNode = AnalysisNode.fromDB(db)
	val analyzer = new Analyzer(geotoolsSmallSubset)

	analyzer.analyze(db)

	db.withoutTransaction { graph =>

		import Prop._

		def printHierarchy(top: Node, rel: RelationshipType, depth: Int = 0) {
			print("  " * depth)
			println(top + ": " + name(top)) //.getProperty("name"))
			for (e <- top.getRelationships(rel, Direction.OUTGOING); val n = e.getEndNode)
				printHierarchy(n, rel, depth + 1)
		}

		def printHierarchies(top: Node, depth: Int, rels: RelationshipType*) {
			print("  " * depth)
			println(top + ": " + name(top))
			for (e <- top.getRelationships(Direction.OUTGOING, rels: _*); val n = e.getEndNode)
				printHierarchies(n, depth + 1, rels: _*)
		}

		println("\nPACKAGE HIERARCHY:")
		printHierarchies(analysisNode.defaultPackage, 0, PackageHierarchy, PackageToClass)

		println("\nFILE HIERARCHY:")
		printHierarchy(analysisNode.defaultFile, FileHierarchy)
	}
}