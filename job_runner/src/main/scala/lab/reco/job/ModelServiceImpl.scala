package lab.reco.job

import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol.Recommendation._
import lab.reco.common.model.EventConfigService
import lab.reco.common.util.Timed
import lab.reco.job.CCOJobHelper._
import lab.reco.job.config.RunnerConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._
import scala.util.Try


trait ModelOperations {
  def runExportEvents(): Future[Long]
  def runTrainModel(hdfsBasePath: String, indicators: List[String]): Future[Long]
  def runImportModel(modelPath: String, esTypeName: String, fieldName: String): Future[Long]
}


class ModelOperationsImpl(runnerConfig: RunnerConfig)(implicit executionContext: ExecutionContext) extends ModelOperations with LazyLogging {
  private def executeCommand(cmd: String): Long = {
    val code = cmd ! ProcessLogger(s => logger.debug(s))
    if (code != 0) {
      throw new RuntimeException(s"[$cmd] returned code [$code]")
    }
    System.currentTimeMillis() // return command execution finish time
  }

  def runExportEvents(): Future[Long] = Future {
    val command = s"${runnerConfig.exportEventsScriptPath}"
    logger.info(s"export events to HDFS command [$command]")
    val result = executeCommand(command)
    logger.info(s"export events to HDFS finished")
    result
  }

  def runTrainModel(hdfsBasePath: String, indicators: List[String]): Future[Long] = Future {
    val command = s"${runnerConfig.trainModelScriptPath} $hdfsBasePath ${indicators.mkString(",")}"
    logger.info(s"train model command [$command]")
    val result = executeCommand(command)
    logger.info(s"train model finished")
    result
  }

  def runImportModel(modelPath: String, esTypeName: String, fieldName: String): Future[Long] = Future {
    val command = s"${runnerConfig.exportModelScriptPath} $modelPath $esTypeName $fieldName"
    logger.info(s"export model to ElasticSearch command [$command]")
    val result = executeCommand(command)
    logger.info(s"export model to ElasticSearch finished")
    result
  }
}

class ModelServiceImpl(eventConfigService: EventConfigService, modelOperations: ModelOperations)
                      (implicit executionContext: ExecutionContext)
  extends ModelService
    with Timed
    with LazyLogging {

  private[job] implicit var jobInfo: Option[Job] = None

  private def canStartNewJob: Boolean = jobInfo.forall { job =>
    job.isFailed || job.isFinished
  }

  override def train(): Future[Task] = {
    if (canStartNewJob) {
      eventConfigService.getIndicatorsConfig().flatMap {
        case Some(config) =>

          val indicators: List[String] = config.primaryIndicator :: config.secondaryIndicators.map(_.name).toList

          val newJob = Job.build(CCOJobHelper.createTasks(indicators))

          jobInfo = Some(newJob)

          modelOperations.runExportEvents()
            .exportEventsFinished(newJob)
            .flatMap {
              _ => modelOperations.runTrainModel(s"/model", indicators)
            }
            .trainModelFinished(newJob)
            .flatMap { _ =>
              eventConfigService.getModelVersion()
            }
            .flatMap { version =>
              val newModelVersion = version + 1

              def runImport(previous: Future[Unit], indicator: String): Future[Unit] = {
                previous.flatMap { _ =>
                  modelOperations.runImportModel(s"/model/similarity-matrix-$indicator", s"$indexName/$typeName", recommendationsField(indicator, newModelVersion.toString))
                    .importModelFinished(newJob, indicator)
                }
              }

              // sequential run is used because we do want to avoid conflicts by updating the same document concurrently
              val result = indicators
                .foldLeft(Future.successful())( runImport )

//              // run in parallel
//              val imports = indicators
//                .map { indicator =>
//                  runImportModel(s"/model/similarity-matrix-$indicator", s"$indexName/$typeName", recommendationsField(indicator, newModelVersion.toString))
//                    .importModelFinished(newJob, indicator)
//                }
//              val result = Future.sequence(imports)


              result.flatMap { _ =>
                eventConfigService.setModelVersion(newModelVersion)
              }
            }

          Future.successful(newJob.currentStatus)
        case None =>
          Future.failed(new RuntimeException("no model config"))
      }
    } else {
      Future.failed(new RuntimeException("cannot start job"))
    }
  }

  override def getStatus: Try[Option[Task]] = Try {
    jobInfo.map(_.currentStatus)
  }
}
