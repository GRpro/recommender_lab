package lab.reco.job

import lab.reco.common.model.EventConfigService
import lab.reco.job.config.RunnerConfig

import scala.concurrent.{ExecutionContext, Future}


trait ModelService {
  def train(): Future[Unit]
}


object ModelService {
  def apply(eventConfigService: EventConfigService, runnerConfig: RunnerConfig)(implicit executionContext: ExecutionContext): ModelService =
    new ModelServiceImpl(eventConfigService, runnerConfig)
}