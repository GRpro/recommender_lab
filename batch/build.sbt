// https://mvnrepository.com/artifact/org.apache.mahout/mahout-spark
libraryDependencies += "org.apache.mahout" %% "mahout-spark" % "0.13.0"

// https://mvnrepository.com/artifact/org.apache.mahout/mahout-math
//libraryDependencies += "org.apache.mahout" % "mahout-math" % "0.13.0"
libraryDependencies += "org.apache.mahout" % "mahout-hdfs" % "0.13.0"


//libraryDependencies += "org.apache.mahout" %% "mahout-spark" % "0.13.0" % "tests" % "compile->test"
//// https://mvnrepository.com/artifact/org.apache.mahout/mahout-math
//libraryDependencies += "org.apache.mahout" % "mahout-math" % "0.13.0"
//
//libraryDependencies += "org.apache.mahout" %% "mahout-math-scala" % "0.13.0"
scalaVersion := "2.10.7"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.2" % Test

//libraryDependencies ++= {
//  val sparkVersion = "2.1.3"
//  Seq(
//    "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
//    "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
//  )
//}

assemblyMergeStrategy in assembly := {
//  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
//  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
//  case "application.conf"                            => MergeStrategy.concat
//  case "unwanted.txt"                                => MergeStrategy.discard
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}
