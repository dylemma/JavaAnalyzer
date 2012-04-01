package com.dylemma.jinfo
import org.neo4j.graphdb.PropertyContainer
import com.dylemma.jinfo.Prop.NeoTypeEvidence

object Prop {
	sealed trait NeoTypeEvidence[-A]
	implicit object AnyValEvidence extends NeoTypeEvidence[AnyVal]
	implicit object StringEvidence extends NeoTypeEvidence[String]
	class ArrayTypeEvidence[A](implicit e: NeoTypeEvidence[A]) extends NeoTypeEvidence[Array[A]]
	implicit def arrayTypeEvidence[A](implicit e: NeoTypeEvidence[A]) = new ArrayTypeEvidence[A]

	val name = Prop[String]("name")
	val lineStart = Prop[Int]("lineStart")
	val lineEnd = Prop[Int]("lineEnd")
	val modifiers = Prop[Int]("modifiers")
}

case class Prop[A](pname: String)(implicit e: NeoTypeEvidence[A]) {
	def apply(pc: PropertyContainer): A = pc.getProperty(pname).asInstanceOf[A]
	def update(pc: PropertyContainer, value: A): Unit = pc.setProperty(pname, value)
}

