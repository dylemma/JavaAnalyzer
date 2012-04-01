name := "JavaParser"

version := "1.0"

scalaVersion := "2.9.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
	"com.google.code.javaparser" % "javaparser" % "1.0.8",
	"com.github.scala-incubator.io" %% "scala-io-core" % "0.4-SNAPSHOT",
	"com.github.scala-incubator.io" %% "scala-io-file" % "0.4-SNAPSHOT",
	"com.typesafe.akka" % "akka-actor" % "2.0",
	"com.typesafe.akka" % "akka-remote" % "2.0",
	"org.neo4j" % "neo4j" % "1.7.M02"
)