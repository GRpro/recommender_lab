package lab.reco.batch

import org.apache.commons.cli.{CommandLine, DefaultParser, HelpFormatter, Options, ParseException, Option => CliOption}
import org.apache.spark.{SparkConf, SparkContext}
import org.elasticsearch.spark._

object ImportEventsJob {

  def main(args: Array[String]): Unit = {
    val options = new Options()
    options.addOption(CliOption.builder("eit").longOpt("es-index-type").hasArg().required()
      .desc("ElasticSearch 'index/type'").build())
    options.addOption(CliOption.builder("eu").longOpt("es-url").hasArg().required()
      .desc("ElasticSearch url").build())
    options.addOption(CliOption.builder("ep").longOpt("es-port").hasArg().required()
      .desc("ElasticSearch port").build())
    options.addOption(CliOption.builder("eun").longOpt("es-username").hasArg().required()
      .desc("ElasticSearch url").build())
    options.addOption(CliOption.builder("eup").longOpt("es-password").hasArg().required()
      .desc("ElasticSearch url").build())
    options.addOption(CliOption.builder("o").longOpt("output").hasArg().required()
      .desc("Export path").build())

    def printUsage =
      new HelpFormatter()
        .printHelp("ImportEventsJob", "Import indexed events from ElasticSearch", options, "", true)

    val cmdLine: CommandLine = try {
      new DefaultParser().parse(options, args)
    } catch {
      case ex: ParseException =>
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

    implicit val spark = new SparkContext(sparkConf)
    val sqlContext = new org.apache.spark.sql.SQLContext(spark)

    spark.hadoopConfiguration.set("mapreduce.fileoutputcommitter.marksuccessfuljobs", "false")
    spark.hadoopConfiguration.set("parquet.enable.summary-metadata", "false")

    val reader = sqlContext.read
      .format("org.elasticsearch.spark.sql")
      .option("es.index.auto.create", "true")
      .option("es.nodes.wan.only", "true")
      .option("es.port", esPort)
      .option("es.net.http.auth.user", esUsername)
      .option("es.net.http.auth.pass", esPassword)
      .option("es.net.ssl", "false")
    .option("es.nodes", esUrl)

    val df = reader.load(esIndexType)

    df.show(1000, false)
    df.write.format("com.databricks.spark.csv")
      .option("header", "true")
      .save(out)

    spark.stop()
  }
}
