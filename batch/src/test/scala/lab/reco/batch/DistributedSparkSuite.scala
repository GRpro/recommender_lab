package lab.reco.batch

import org.apache.log4j.{Level, Logger}
import org.apache.mahout.math.drm.DistributedContext
import org.apache.mahout.sparkbindings._
import org.apache.spark.SparkConf
import org.scalatest.{ConfigMap, Suite}

import scala.collection.JavaConversions._

trait DistributedSparkSuite extends DistributedMahoutSuite with LoggerConfiguration {
  this: Suite =>

  protected implicit var mahoutCtx: DistributedContext = _
  protected var masterUrl = null.asInstanceOf[String]

  protected def initContext() {
    masterUrl = System.getProperties.getOrElse("test.spark.master", "local[1]")
    val isLocal = masterUrl.startsWith("local")
    mahoutCtx = mahoutSparkContext(masterUrl = this.masterUrl,
      appName = "MahoutUnitTests",
      // Do not run MAHOUT_HOME jars in unit tests.
      addMahoutJars = !isLocal,
      sparkConf = new SparkConf()
        .set("spark.kryoserializer.buffer.mb", "40")
        .set("spark.kryoserializer.buffer", "40")
        .set("spark.akka.frameSize", "30")
        .set("spark.default.parallelism", "10")
        .set("spark.executor.memory", "2G")
    )
    // Spark reconfigures logging. Clamp down on it in tests.
    Logger.getRootLogger.setLevel(Level.ERROR)
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  }

  protected def resetContext() {
    if (mahoutCtx != null) {
      try {
        mahoutCtx.close()
      } finally {
        mahoutCtx = null
      }
    }
  }

  override protected def beforeEach() {
    super.beforeEach()
    //    initContext()
  }

  override protected def afterAll(configMap: ConfigMap): Unit = {
    super.afterAll(configMap)
    resetContext()
  }

  override protected def beforeAll(configMap: ConfigMap): Unit = {
    super.beforeAll(configMap)
    initContext()
  }

  override protected def afterEach() {
    //    resetContext()
    super.afterEach()
  }
}
