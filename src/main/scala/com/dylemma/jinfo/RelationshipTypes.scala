package com.dylemma.jinfo

import org.neo4j.graphdb.RelationshipType

/** This is supposed to be extended as case objects, so I'm marking it abstract */
abstract class Rel(val name: String) extends RelationshipType

case object RefToAnalysis extends Rel("refToAnalysis")
case object PackageHierarchy extends Rel("pkg-hierarchy")
case object PackageToClass extends Rel("pkgToClass")
case object FileHierarchy extends Rel("file-hierarchy")