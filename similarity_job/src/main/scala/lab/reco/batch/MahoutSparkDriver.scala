package lab.reco.batch

import org.apache.mahout.math.drm.DistributedContext
import org.apache.mahout.sparkbindings._
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.TraversableOnce

/**
  * Extend this class to create a Mahout CLI driver. Minimally you must override process and main.
  * Also define a Map of options for the command line parser. The following template may help:
  * {{{
  * object SomeDriver extends MahoutDriver {
  *
  *   // define only the options specific to this driver, inherit the generic ones
  *   private final val SomeOptions = HashMap[String, Any](
  *       "maxThings" -> 500,
  *       "minThings" -> 100,
  *       "appName" -> "SomeDriver")
  *
  *   override def main(args: Array[String]): Unit = {
  *
  *     val parser = new MahoutOptionParser(programName = "shortname") {
  *       head("somedriver", "Mahout 1.0-SNAPSHOT")
  *
  *       // Input output options, non-driver specific
  *       parseIOOptions()
  *
  *       // Algorithm specific options
  *       // Add in the new options
  *       opts = opts ++ SomeOptions
  *       note("\nAlgorithm control options:")
  *       opt[Int]("maxThings") abbr ("mt") action { (x, options) =>
  *         options + ("maxThings" -> x) ...
  *     }
  *     parser.parse(args, parser.opts) map { opts =>
  *       parser.opts = opts
  *       process()
  *     }
  *   }
  *
  *   override def process: Unit = {
  *     start() // override to change the default Kryo or SparkConf before the distributed context is created
  *     // do the work here
  *     stop()
  *   }
  *
  * }}}
  */
abstract class MahoutSparkDriver extends MahoutDriver {


  implicit var sparkConf = new SparkConf()

  /**
    * Creates a Spark context to run the job inside.
    * Override to set the SparkConf values specific to the job,
    * these must be set before the context is created.
    */
  override protected def start(): Unit = {
    if (!_useExistingContext) {
      sparkConf.set("spark.kryo.referenceTracking", "false")
        .set("spark.kryoserializer.buffer.mb", "200m") // this is default for Mahout optimizer, change it with -D option
        // the previous has been marked deprecated as of Spark 1.4 by the below line,
        // remove the above line when Spark finally retires above for below
        .set("spark.kryoserializer.buffer", "200m")


      if (parser.opts("sparkExecutorMem").asInstanceOf[String] != "")
        sparkConf.set("spark.executor.memory", parser.opts("sparkExecutorMem").asInstanceOf[String])
      //else leave as set in Spark config
      mc = mahoutSparkContext(
        masterUrl = parser.opts("master").asInstanceOf[String],
        appName = parser.opts("appName").asInstanceOf[String],
        sparkConf = sparkConf)
    }
  }

  def mahoutSparkContext(masterUrl: String, appName: String, customJars: TraversableOnce[String] = Nil,
                         sparkConf: SparkConf = new SparkConf(), addMahoutJars: Boolean = true):
  SparkDistributedContext = {

    sparkConf.setAppName(appName).setMaster(masterUrl)
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrator", "org.apache.mahout.sparkbindings.io.MahoutKryoRegistrator")

    if (System.getenv("SPARK_HOME") != null) {
      sparkConf.setSparkHome(System.getenv("SPARK_HOME"))
    }

    new SparkDistributedContext(new SparkContext(config = sparkConf))
  }

  /**
    * Call this before start to use an existing context as when running multiple drivers from a scalatest suite.
    *
    * @param context An already set up context to run against
    */
  def useContext(context: DistributedContext): Unit = {
    _useExistingContext = true
    mc = context
    sparkConf = mc.getConf
  }
}
