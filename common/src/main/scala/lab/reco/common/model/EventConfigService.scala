package lab.reco.common.model

import com.sksamuel.elastic4s.http.ElasticClient

import scala.concurrent.{ExecutionContext, Future}


case class IndicatorConfig(name: String, priority: Int)

case class IndicatorsConfig(primaryIndicator: String, secondaryIndicators: Seq[IndicatorConfig])

trait EventConfigService {

  def setIndicatorsConfig(modelConfig: IndicatorsConfig): Future[Unit]

  def getIndicatorsConfig(): Future[Option[IndicatorsConfig]]

  def getModelVersion(): Future[Int]

  def setModelVersion(version: Int): Future[Unit]
}


object EventConfigService {
  def apply(esClient: ElasticClient)(implicit executionContext: ExecutionContext): EventConfigService =
    new EventConfigServiceImpl(esClient)
}