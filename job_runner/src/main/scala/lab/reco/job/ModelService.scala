package lab.reco.job

import lab.reco.common.model.EventConfigService
import lab.reco.job.config.RunnerConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class TaskStatus(name: String, completedAt: Option[Long], error: Option[String])
case class JobStatus(tasks: Set[TaskStatus],
                     startedAt: Long,
                     elapsed: Long,
                     hasFailures: Boolean,
                     completedAt: Option[Long])

trait ModelService {
  def train(): Future[JobStatus]

  def getStatus: Try[JobStatus]

}


object ModelService {
  def apply(eventConfigService: EventConfigService, runnerConfig: RunnerConfig)(implicit executionContext: ExecutionContext): ModelService =
    new ModelServiceImpl(eventConfigService, runnerConfig)
}