package lab.reco.job

import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol
import lab.reco.common.model.EventConfigService
import lab.reco.common.util.Timed
import lab.reco.job.config.RunnerConfig

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.sys.process._
import scala.util.{Failure, Success, Try}


class ModelServiceImpl(eventConfigService: EventConfigService, runnerConfig: RunnerConfig)
                      (implicit executionContext: ExecutionContext)
  extends ModelService
    with Timed
    with LazyLogging {

  private val typeName = Protocol.Recommendation.typeName

  private case class JobInternal(startedAt: Long, trainModelsFutures: Map[String, Future[Long]])

  private var jobInfo: Option[JobInternal] = None


  private def isActiveJob: Boolean =
    jobInfo.exists(_.trainModelsFutures.mapValues(!_.isCompleted).exists(_._2))

  private def validateNoActiveJob: Unit = {
    if (isActiveJob)
      throw new RuntimeException("previous job in progress")
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

  private def runTrainModel(modelHdfsPath: String, primaryIndicator: String, secondaryIndicator: Option[String]): Future[Long] = Future {
    val command = s"${runnerConfig.trainModelScriptPath} $modelHdfsPath $primaryIndicator ${secondaryIndicator.getOrElse("")}"
    logger.info(s"train model command [$command]")
    executeCommand(command)
  }

  private def runImportModel(modelPath: String, esTypeName: String): Future[Long] = Future {
    val command = s"${runnerConfig.exportModelScriptPath} $modelPath $esTypeName"
    logger.info(s"export model to ElasticSearch command [$command]")
    executeCommand(command)
  }

  override def train(): Future[JobStatus] = {
    validateNoActiveJob

    eventConfigService.getConfig().map {
      case Some(config) =>

        var promiseMap: Map[String, Promise[Long]] = Map(config.primaryIndicator -> Promise())
        config.secondaryIndicators.foreach {
          promiseMap += _.name -> Promise()
        }

        def taskSuccessful(taskName: String, completedAt: Long): Unit = promiseMap(taskName).success(completedAt)
        def taskFailed(taskName: String, exception: Exception): Unit = promiseMap(taskName).failure(exception)


        runExportEvents().onComplete {
          case Success(_) =>

            @inline
            def trainModel(indicatorName: String): Future[Long] =
              runTrainModel(s"/model-$indicatorName", config.primaryIndicator, None)

            @inline
            def trainModelCCO(indicatorName: String): Future[Long] =
              runTrainModel(s"/model-$indicatorName", config.primaryIndicator, Some(indicatorName))

            @inline
            def importModel(indicatorName: String, modelName: String, indexName: String): Future[Long] =
              runImportModel(s"/model-$indicatorName/$modelName", s"$indexName/$typeName")


            if (config.secondaryIndicators.nonEmpty) {

              val firstIndicator = config.secondaryIndicators.head

              trainModelCCO(firstIndicator.name)
                .map { _ =>
                  importModel(firstIndicator.name, "similarity-matrix", config.primaryIndicator)
                    .map { completedAt =>
                      taskSuccessful(config.primaryIndicator, completedAt)
                    }.recover {
                      case e =>
                        taskFailed(config.primaryIndicator, new RuntimeException(s"failed to train model for indicator [${config.primaryIndicator}]: ${e.getMessage}"))
                    }

                  importModel(firstIndicator.name, "cross-similarity-matrix", firstIndicator.name)
                    .map { completedAt =>
                      taskSuccessful(firstIndicator.name, completedAt)
                    }.recover {
                      case e =>
                        taskFailed(firstIndicator.name, new RuntimeException(s"failed to train model for indicator [${firstIndicator.name}]: ${e.getMessage}"))
                    }
              } recover {
                case e =>
                  taskFailed(firstIndicator.name, new RuntimeException(s"failed to train model for indicator [${firstIndicator.name}]: ${e.getMessage}"))
              }

              config.secondaryIndicators.tail.foreach { indicator =>
                trainModelCCO(indicator.name).map { _ =>
                  importModel(indicator.name, "cross-similarity-matrix", indicator.name)
                    .map { completedAt =>
                      taskSuccessful(indicator.name, completedAt)
                    }.recover {
                      case e =>
                        taskFailed(indicator.name, new RuntimeException(s"failed to train model for indicator [${indicator.name}]: ${e.getMessage}"))
                    }
                }
              }
            } else {
              trainModel(config.primaryIndicator).map { _ =>
                importModel(config.primaryIndicator, "cross-similarity-matrix", config.primaryIndicator)
                  .map { completedAt =>
                    taskSuccessful(config.primaryIndicator, completedAt)
                  }.recover {
                    case e =>
                      taskFailed(config.primaryIndicator, new RuntimeException(s"failed to train model for indicator [${config.primaryIndicator}]: ${e.getMessage}"))
                }
              }
            }

          case Failure(e) =>
            promiseMap.mapValues {
              _.complete(Failure(new RuntimeException(s"events import failed: ${e.getMessage}")))
            }
        }

        jobInfo = Some(JobInternal(System.currentTimeMillis(), promiseMap.mapValues(_.future)))
      case None =>
        throw new RuntimeException("no model config")
    }.map { _ =>
      getStatus.get
    }
  }

  override def getStatus: Try[JobStatus] = Try {
    jobInfo match {
      case Some(job) =>
        var hasFailures: Boolean = false
        var allCompleted: Boolean = true
        var maxCompletedAt: Option[Long] = None

        val tasks = job.trainModelsFutures.map {
          case (indicatorName, completion) =>
            completion.value match {
              case Some(Success(completedAt)) =>
                maxCompletedAt = Some(math.max(completedAt, maxCompletedAt.getOrElse(-1L)))
                TaskStatus(indicatorName, Some(completedAt), None)
              case Some(Failure(e)) =>
                hasFailures = true
                TaskStatus(indicatorName, None, Some(e.getMessage))
              case None =>
                allCompleted = false
                TaskStatus(indicatorName, None, None)
            }

        }

        val elapsedTime = if (allCompleted) {
          maxCompletedAt.get - job.startedAt
        } else {
          System.currentTimeMillis() - job.startedAt
        }

        val completedAt = if (allCompleted) {
          maxCompletedAt
        } else {
          None
        }

        JobStatus(tasks.toSet, job.startedAt, elapsedTime, hasFailures, completedAt)
      case None =>
        throw new RuntimeException("no model training")
    }
  }

}
