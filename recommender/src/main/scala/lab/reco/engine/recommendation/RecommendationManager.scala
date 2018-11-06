package lab.reco.engine.recommendation

import com.sksamuel.elastic4s.http.ElasticClient
import lab.reco.common.model.EventConfigService

import scala.concurrent.{ExecutionContext, Future}

trait RecommendationManager {

  def getRecommendations(objectId: String, size: Int): Future[SimilarObjectsRecommendation]

  def clearAllRecommendations(): Future[Unit]
}

object RecommendationManager {

  def apply(esClient: ElasticClient, eventConfigService: EventConfigService)(implicit executionContext: ExecutionContext): RecommendationManager = {
    new RecommendationManagerImpl(esClient, eventConfigService)
  }

}