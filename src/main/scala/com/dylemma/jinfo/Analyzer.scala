package com.dylemma.jinfo
import scalax.file.Path
import scalax.file.PathSet
import japa.parser.JavaParser
import scala.collection.JavaConversions._
import japa.parser.ast.body.TypeDeclaration
import org.neo4j.graphdb.Node
import japa.parser.ast.body.ClassOrInterfaceDeclaration

class Analyzer(rootPath: Path) {

	val paths = rootPath ** { p: Path => p.extension == Some("java") }

	def analyze(db: DB) = {
		// lookup/create the analysis node (manages its own TX)
		val anode = AnalysisNode.fromDB(db)

		// get a map from Path to parsed CompilationUnit
		val pcus = {
			for {
				path <- paths.iterator
				val parsed = path.inputStream.acquireFor(JavaParser.parse)
				cu <- parsed match {
					case Left(_) => None
					case Right(cu) => Some(cu)
				}
			} yield path -> cu
		}.toMap

		//Analysis Step 1: Persist all of the package nodes
		db.withTransaction { graph =>
			for ((_, cu) <- pcus) {
				val pkg = Option(cu.getPackage).map(_.getName.toString).getOrElse("")
				for (pkgNode <- anode.pkg(pkg))
					Prop.kind(pkgNode) = NodeKind.Package
			}
		}

		//Analysis Step 2: Persist all of the file nodes
		db.withTransaction { graph =>
			for ((path, _) <- pcus) {
				val relPath = path.relativize(rootPath)
				for (fileNode <- anode.file(relPath))
					Prop.kind(fileNode) = NodeKind.File
			}
		}

		//helper function for step 3
		def storeTypeInfo(anode: AnalysisNode, node: Node, typeDec: TypeDeclaration): Unit = {
			Prop.name(node) = typeDec.getName
			Prop.lineStart(node) = typeDec.getBeginLine
			Prop.lineEnd(node) = typeDec.getEndLine
			Prop.modifiers(node) = typeDec.getModifiers
			Prop.inferred(node) = false
			Prop.kind(node) = NodeKind.Type
			//etc..

			//store the javadoc
			for (jdc <- Option(typeDec.getJavaDoc)) {
				val rawContent = jdc.getContent
				//strip any whitespace then * from the start of each line
				Prop.javadoc(node) = rawContent.split('\n').map {
					_.dropWhile(_.isWhitespace).dropWhile(_ == '*')
				}.mkString("\n")
			}

			//recurse for inner types
			for {
				innerType <- Helpers.innerTypes(typeDec)
				innerTypeName <- Option(innerType.getName)
				innerNode <- anode.nodeFromPath(node, List(innerTypeName), ClassToInnerClass, createIfMissing = true)
			} storeTypeInfo(anode, innerNode, innerType)
		}

		//Analysis Step 3: Persist all of the named type nodes
		db.withTransaction { graph =>
			for {
				(path, cu) <- pcus
				typeDec <- Helpers.types(cu)
				typeName <- Option(typeDec.getName)
				tnode <- anode.typeNode(typeName, cu.getPackage, true)
			} storeTypeInfo(anode, tnode, typeDec)
		}

		//Analysis Step 4: Persist the imported things
		db.withTransaction { graph =>
			for {
				(_, cu) <- pcus
				i <- Helpers.imports(cu)
				importName <- Option(i.getName)
			} {
				val importSegments = importName.toString.split('.').toList
				val solidImport = anode.lookupNode(anode.defaultPackage, importSegments, Seq(PackageHierarchy, PackageToClass, ClassToInnerClass))

				//if the import doesn't refer to something in the source,
				//then create an "import node" for it
				solidImport orElse anode.importNode(importName.toString)
			}
		}

		//Analysis Step 5: Extends/Implements resolution
		db.withTransaction { graph =>
			for {
				(path, cu) <- pcus
				typeDec <- Helpers.types(cu)
				tnode <- anode.typeNode(typeDec.getName, cu.getPackage, false)
			} {
				//now tnode represents t
				//and we want to add extends/implements links from it
				typeDec match {
					case c: ClassOrInterfaceDeclaration =>
					/*TODO: use the `getExtends` and `getImplements` values
						 to make links to the right nodes. There should be NO failures,
						 since every import and type is already in the DB by now */
					case _ => //noop
				}
			}
		}

	}

}