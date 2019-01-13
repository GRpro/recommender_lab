
resolvers ++= Seq("Spark Packages Repo" at "https://dl.bintray.com/spark-packages/maven")

libraryDependencies ++= {
  val sparkVersion = "2.1.3"
  Seq(
    "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
  )
}

libraryDependencies += "org.elasticsearch" %% "elasticsearch-spark-20" % "6.4.2"
libraryDependencies += "com.databricks" %% "spark-csv" % "1.5.0"
