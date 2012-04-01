package com.dylemma.jinfo

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.EmbeddedGraphDatabase

class DB private (path: String) {
	private lazy val db = {
		val result = new EmbeddedGraphDatabase(path)
		val shutdownHook = new Thread() {
			override def run = {
				println("Shutting down graph: " + path)
				result.shutdown
			}
		}

		Runtime.getRuntime.addShutdownHook(shutdownHook)
		result
	}

	def withTransaction[A](body: GraphDatabaseService => A) = {
		val tx = db.beginTx
		try {
			val result = body(db)
			tx.success
			result
		} finally {
			tx.finish
		}
	}

	def withoutTransaction[A](body: GraphDatabaseService => A) = body(db)
}

object DB {
	private var dbs: Map[String, DB] = Map()

	def apply(path: String) = dbs.get(path) match {
		case Some(db) => db
		case None =>
			val db = new DB(path)
			dbs += path -> db
			db
	}
}