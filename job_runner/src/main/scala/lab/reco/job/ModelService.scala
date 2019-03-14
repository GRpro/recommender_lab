package lab.reco.job

import lab.reco.common.model.EventConfigService
import lab.reco.job.config.RunnerConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait ModelService {
  def train(): Future[Task]

  def getStatus: Try[Option[Task]]

}


object ModelService {
  def apply(eventConfigService: EventConfigService, runnerConfig: RunnerConfig)(implicit executionContext: ExecutionContext): ModelService =
    new ModelServiceImpl(eventConfigService, new ModelOperationsImpl(runnerConfig))
}