package com.dylemma.jinfo

import scalax.file.Path
import japa.parser.JavaParser
import scala.collection.JavaConversions._
import java.lang.reflect.Modifier
import japa.parser.ast.body.MethodDeclaration
import japa.parser.ast.body.ConstructorDeclaration

object TestyTime {

	val geotoolsRoot = Path("test-inputs", "geotools", "modules", "library")

	val paths = geotoolsRoot ** { p: Path =>
		p.extension == Some("java")
	}

	//	for (p <- paths) println(p.relativize(geotoolsRoot))

	println("Found " + paths.size + " paths")

	val parsed = paths.head.inputStream.acquireFor { in =>
		JavaParser.parse(in)
	}

	parsed match {
		case Left(errors) => errors foreach println
		case Right(cu) =>
			println(cu)
			//			println(cu.getPackage.getName)
			for (t <- cu.getTypes) {
				println(t.getName)
				println(t.getModifiers)
				t.getMembers map {
					case c: ConstructorDeclaration =>
						println("Constructor, params = " + c.getParameters.toList)
					case m: MethodDeclaration =>
						println("Method: " + m.getName + " + params(" + m.getParameters + ") + returns " + m.getType)
					case _ =>
				}
			}
	}

}