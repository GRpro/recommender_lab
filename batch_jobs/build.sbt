


libraryDependencies ++= {
  val sparkVersion = "1.6.3"
  Seq(
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.apache.spark" %% "spark-core" % sparkVersion/*,
    "org.apache.spark" %% "spark-mllib" % sparkVersion*/
  )
}

libraryDependencies += "org.elasticsearch" %% "elasticsearch-spark" % "2.4.5"

//libraryDependencies += "org.elasticsearch" %% "elasticsearch-spark-20" % "6.4.2"
libraryDependencies += "com.databricks" %% "spark-csv" % "1.5.0"
libraryDependencies += "commons-cli" % "commons-cli" % "1.4"


//libraryDependencies += "org.apache.mahout" %% "mahout-spark" % "0.13.0" //exclude("org.apache.spark", "spark-core") // exclude("org.apache.spark", "spark-mllib")
//libraryDependencies += "org.apache.mahout" %% "mahout-spark-shell_2.10" % "0.12.2"
