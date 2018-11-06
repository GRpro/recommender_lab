package lab.reco.job

import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol
import lab.reco.common.model.{EventConfigService, IndicatorConfig}
import lab.reco.common.util.Timed
import lab.reco.job.config.RunnerConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

class ModelServiceImpl(eventConfigService: EventConfigService, runnerConfig: RunnerConfig)
                      (implicit executionContext: ExecutionContext)
  extends ModelService
    with Timed
    with LazyLogging {

  private val typeName = Protocol.Recommendation.typeName

  private def executeCommand(cmd: String): Int = {
    cmd ! ProcessLogger(s => logger.debug(s))
  }
  private def exportEventsToHdfs(): Unit = {
    val command = s"${runnerConfig.exportEventsScriptPath}"
    logger.info(s"export events to HDFS command [$command]")
    val code = executeCommand(command)
    if (code != 0) {
      logger.warn(s"[$command] returned code [$code]")
    }
  }

  private def trainModel(modelHdfsPath: String, primaryIndicator: String, secondaryIndicator: Option[String]): Unit = {
    val command = s"${runnerConfig.trainModelScriptPath} $modelHdfsPath $primaryIndicator ${secondaryIndicator.getOrElse("")}"
    logger.info(s"train model command [$command]")
    val code = executeCommand(command)
    if (code != 0) {
      logger.warn(s"[$command] returned code [$code]")
    }
  }

  private def exportModel(modelPath: String, esTypeName: String): Unit = {
    val command = s"${runnerConfig.exportModelScriptPath} $modelPath $esTypeName"
    logger.info(s"export model to ElasticSearch command [$command]")
    val code = executeCommand(command)
    if (code != 0) {
      logger.warn(s"[$command] returned code [$code]")
    }
  }

  override def train(): Future[Unit] = {
    eventConfigService.getConfig().map { modelConfig =>
      // step 1
      exportEventsToHdfs()

      modelConfig match {
        case Some(config) if config.secondaryIndicators.nonEmpty =>
          def trainAndExport(indicatorConfig: IndicatorConfig, exportFirst: Boolean): Unit = {
            // step 2
            trainModel(s"/model-${indicatorConfig.name}", config.primaryIndicator, Some(indicatorConfig.name))

            // step 3
            exportModel(s"/model-${indicatorConfig.name}/cross-similarity-matrix", s"${indicatorConfig.name}/$typeName")
            if (exportFirst) {
              exportModel(s"/model-${indicatorConfig.name}/similarity-matrix", s"${config.primaryIndicator}/$typeName")
            }
          }

          trainAndExport(config.secondaryIndicators.head, exportFirst = true)

          config.secondaryIndicators.tail.foreach {
            trainAndExport(_, exportFirst = false)
          }
        case Some(config) =>
          trainModel("/model", config.primaryIndicator, None)
          exportModel("/model/similarity-matrix", s"${config.primaryIndicator}/$typeName")
        case None =>
          logger.info("No config for model, ignore.")

      }
    }
  }
}
