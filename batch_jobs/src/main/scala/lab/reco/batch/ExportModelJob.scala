package lab.reco.batch

import org.apache.commons.cli.{BasicParser, CommandLine, HelpFormatter, Options, Option => CliOption}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Row, SparkSession}

object ExportModelJob {

  def main(args: Array[String]): Unit = {
    val options = new Options()
    options.addOption(new CliOption("eit", "es-index-type", true, "ElasticSearch 'index/type'"))
    options.addOption(new CliOption("eu", "es-url", true, "ElasticSearch url"))
    options.addOption(new CliOption("ep", "es-port", true, "ElasticSearch port"))
    options.addOption(new CliOption("eun", "es-username", true, "ElasticSearch username"))
    options.addOption(new CliOption("eup", "es-password", true, "ElasticSearch password"))
    options.addOption(new CliOption("i", "input", true, "Input path"))

    def printUsage =
      new HelpFormatter()
        .printHelp("ImportEventsJob", "Import indexed events from ElasticSearch", options, "", true)

    val cmdLine: CommandLine = try {
      new BasicParser().parse(options, args)
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        printUsage
        sys.exit(1)
    }

    if (cmdLine.hasOption('h')) {
      printUsage
      sys.exit(0)
    }

    val esIndexType: String = cmdLine.getOptionValue("eit")
    val esUrl: String = cmdLine.getOptionValue("eu")
    val esPort: String = cmdLine.getOptionValue("ep")
    val esUsername: String = cmdLine.getOptionValue("eun")
    val esPassword: String = cmdLine.getOptionValue("eup")
    val in: String = cmdLine.getOptionValue("i")

    val sparkConf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Spark dataset analyzer")

    implicit val spark = SparkSession.builder.config(sparkConf).getOrCreate()
    import spark.implicits._

    spark.sparkContext.hadoopConfiguration.set("mapreduce.fileoutputcommitter.marksuccessfuljobs", "false")
    spark.sparkContext.hadoopConfiguration.set("parquet.enable.summary-metadata", "false")

    val reader = spark.read.format("text")

    val df = reader.load(in).map {
      case Row(text: String) =>
        val splitted = text.split("\t")
        val objectId = splitted(0)
        val splittedRecommendations = splitted(1).split(" ").map { rec =>
          val sp = rec.split(":")
          (sp(0), sp(1).toDouble)
        }.filterNot { _._1 == objectId }

        val recommendations = splittedRecommendations.map { _._1 }
        val preferences = splittedRecommendations.map { _._2 }
        (objectId, recommendations, preferences)
    }.toDF("objectId", "recommendations", "preferences")

    df.write
      .format("org.elasticsearch.spark.sql")
      .option("es.index.auto.create", "true")
      .option("es.nodes.wan.only", "true")
      .option("es.port", esPort)
      .option("es.net.http.auth.user", esUsername)
      .option("es.net.http.auth.pass", esPassword)
      .option("es.net.ssl", "false")
      .mode("overwrite")
      .option("es.nodes", esUrl)
      .save(esIndexType)

    spark.stop()
  }
}
