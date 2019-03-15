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
    options.addOption(new CliOption("n", "name", true, "Field name"))

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
    val name: String = cmdLine.getOptionValue("n")

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

        val splittedRecommendations: Array[(String, Double)] = if (splitted.length > 1) {
          splitted(1).split(" ").map { rec =>
            val sp = rec.split(":")
            (sp(0), sp(1).toDouble)
          }.filterNot { _._1 == objectId }
        } else {
          Array.empty
        }

        val recommendations = splittedRecommendations
          .map { _._1 }
        (objectId, recommendations)
    }.toDF("objectId", name).withColumn("id", $"objectId")

    df.write
      .format("org.elasticsearch.spark.sql")
      .option("es.index.auto.create", "true")
      .option("es.nodes.wan.only", "true")
      .option("es.port", esPort)
      .option("es.net.http.auth.user", esUsername)
      .option("es.net.http.auth.pass", esPassword)
      .option("es.net.ssl", "false")
      .option("es.mapping.id", "id")
      .option("es.mapping.exclude", "id")
      .option("es.write.operation", "upsert")
      .option("es.update.retry.on.conflict", "5")
      .option("es.batch.write.retry.count", "5")
      .mode("append")
      .option("es.nodes", esUrl)
      .save(esIndexType)

    spark.stop()
  }
}
