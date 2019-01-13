package lab.reco.batch

import lab.reco.common.Protocol.Event._
import org.apache.commons.cli.{BasicParser, CommandLine, HelpFormatter, Options, Option => CliOption}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object ImportEventsJob {

  def main(args: Array[String]): Unit = {
    val options = new Options()
    options.addOption(new CliOption("eit", "es-index-type", true, "ElasticSearch 'index/type'"))
    options.addOption(new CliOption("eu", "es-url", true, "ElasticSearch url"))
    options.addOption(new CliOption("ep", "es-port", true, "ElasticSearch port"))
    options.addOption(new CliOption("eun", "es-username", true, "ElasticSearch username"))
    options.addOption(new CliOption("eup", "es-password", true, "ElasticSearch password"))
    options.addOption(new CliOption("o", "output", true, "Export path"))

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
    val out: String = cmdLine.getOptionValue("o")

    val sparkConf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Spark dataset analyzer")

    implicit val spark = SparkSession.builder.config(sparkConf).getOrCreate()

    spark.sparkContext.hadoopConfiguration.set("mapreduce.fileoutputcommitter.marksuccessfuljobs", "false")
    spark.sparkContext.hadoopConfiguration.set("parquet.enable.summary-metadata", "false")

    val reader = spark.read
      .format("org.elasticsearch.spark.sql")
      .option("es.index.auto.create", "true")
      .option("es.nodes.wan.only", "true")
      .option("es.port", esPort)
      .option("es.net.http.auth.user", esUsername)
      .option("es.net.http.auth.pass", esPassword)
      .option("es.net.ssl", "false")
      .option("es.nodes", esUrl)

    val df = reader.load(esIndexType).select(subjectIdField, indicatorField, objectIdField)

    df.write.format("com.databricks.spark.csv")
      .option("header", "true")
      .mode("overwrite")
      .save(out)

    spark.stop()
  }
}
