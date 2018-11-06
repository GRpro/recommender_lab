package lab.reco.common.model

import com.sksamuel.elastic4s.http.ElasticClient

import scala.concurrent.{ExecutionContext, Future}


case class IndicatorConfig(name: String, priority: Int)

case class ModelConfig(primaryIndicator: String, secondaryIndicators: Seq[IndicatorConfig])

trait EventConfigService {

  def storeConfig(modelConfig: ModelConfig): Future[Unit]

  def getConfig(): Future[Option[ModelConfig]]
}


object EventConfigService {
  def apply(esClient: ElasticClient)(implicit executionContext: ExecutionContext): EventConfigService =
    new EventConfigServiceImpl(esClient)
}