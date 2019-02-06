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

class ModelServiceImpl(eventConfigService: EventConfigService, runnerConfig: RunnerConfig)
                      (implicit executionContext: ExecutionContext)
  extends ModelService
    with Timed
    with LazyLogging {

  private implicit var jobInfo: Option[Job] = None

  private def canStartNewJob: Boolean = jobInfo.forall { job =>
    job.isFailed || job.isFinished
  }


  private def executeCommand(cmd: String): Long = {
    val code = cmd ! ProcessLogger(s => logger.debug(s))
    if (code != 0) {
      throw new RuntimeException(s"[$cmd] returned code [$code]")
    }
    System.currentTimeMillis() // return command execution finish time
  }

  private def runExportEvents(): Future[Long] = Future {
    val command = s"${runnerConfig.exportEventsScriptPath}"
    logger.info(s"export events to HDFS command [$command]")
    executeCommand(command)
  }

  private def runTrainModel(hdfsBasePath: String, indicators: List[String]): Future[Long] = Future {
    val command = s"${runnerConfig.trainModelScriptPath} $hdfsBasePath ${indicators.mkString(",")}"
    logger.info(s"train model command [$command]")
    executeCommand(command)
  }

  private def runImportModel(modelPath: String, esTypeName: String, fieldName: String): Future[Long] = Future {
    val command = s"${runnerConfig.exportModelScriptPath} $modelPath $esTypeName $fieldName"
    logger.info(s"export model to ElasticSearch command [$command]")
    executeCommand(command)
  }

  override def train(): Future[Task] = {
    if (canStartNewJob) {
      eventConfigService.getIndicatorsConfig().flatMap {
        case Some(config) =>

          val indicators: List[String] = config.primaryIndicator :: config.secondaryIndicators.map(_.name).toList

          val newJob = Job.build(CCOJobHelper.createTasks(indicators))

          jobInfo = Some(newJob)

          runExportEvents()
            .exportEventsFinished(newJob)
            .flatMap {
              _ => runTrainModel(s"/model", indicators)
            }
            .trainModelFinished(newJob)
            .flatMap { _ =>
              eventConfigService.getModelVersion()
            }
            .flatMap { version =>
              val newModelVersion = version + 1

              def runImport(previous: Future[Unit], indicator: String): Future[Unit] = {
                previous.flatMap { _ =>
                  runImportModel(s"/model/similarity-matrix-$indicator", s"$indexName/$typeName", recommendationsField(indicator, newModelVersion.toString))
                    .importModelFinished(newJob, indicator)
                }
              }

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
