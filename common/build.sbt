

// major.minor are in sync with the elasticsearch releases
val elastic4sVersion = "6.3.7"
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,

  // for the http client
  "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sVersion

//  // if you want to use reactive streams
//  "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elastic4sVersion,
//
//  // testing
//  "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion % "test",
//  "com.sksamuel.elastic4s" %% "elastic4s-embedded" % elastic4sVersion % "test"
)

libraryDependencies ++= Seq(
  "io.spray" %% "spray-json" % "1.3.3",
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)


// libraryDependencies += "com.github.swagger-akka-http" %% "swagger-akka-http" % "1.0.0"

libraryDependencies += "org.scalamock" %% "scalamock" % "4.1.0" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % Test
libraryDependencies += "pl.allegro.tech" % "embedded-elasticsearch" % "2.7.0" % Test

