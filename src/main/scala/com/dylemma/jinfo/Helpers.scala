package com.dylemma.jinfo
import scala.collection.JavaConverters._
import japa.parser.ast.body.TypeDeclaration
import japa.parser.ast.CompilationUnit
import japa.parser.ast.ImportDeclaration
import japa.parser.ast.body.BodyDeclaration

object Helpers {
	def safeIterator[A](list: java.util.List[A]) = list match {
		case null => Iterator.empty
		case list => list.iterator.asScala
	}

	def types(cu: CompilationUnit): Iterator[TypeDeclaration] = safeIterator(cu.getTypes)

	def imports(cu: CompilationUnit): Iterator[ImportDeclaration] = safeIterator(cu.getImports)

	def members(typeDec: TypeDeclaration): Iterator[BodyDeclaration] = safeIterator(typeDec.getMembers)

	def innerTypes(typeDec: TypeDeclaration): Iterator[TypeDeclaration] = members(typeDec).collect { case t: TypeDeclaration => t }
}