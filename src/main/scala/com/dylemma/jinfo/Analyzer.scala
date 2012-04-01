package com.dylemma.jinfo
import scalax.file.Path
import scalax.file.PathSet
import japa.parser.JavaParser
import scala.collection.JavaConversions._

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
				val pkgNode = anode.pkg(pkg)
			}
		}

		//Analysis Step 2: Persist all of the file nodes
		db.withTransaction { graph =>
			for ((path, _) <- pcus) {
				val relPath = path.relativize(rootPath)
				val fileNode = anode.file(relPath)
			}
		}

		//Analysis Step 3: Persist all of the type nodes
		db.withTransaction { graph =>
			for {
				(path, cu) <- pcus
				tList <- Option(cu.getTypes())
				t <- tList
				tnode <- anode.typeNode(t.getName, cu.getPackage, true)
			} {
				import Prop._
				name(tnode) = t.getName
				lineStart(tnode) = t.getBeginLine
				lineEnd(tnode) = t.getEndLine

				println(name(tnode))
				println(lineStart(tnode))
				println(lineEnd(tnode))
				//etc..
			}
		}

	}

}