package lab.reco.common

import scala.concurrent.{ExecutionContext, Future}

trait RecommendationManager {

  def getSimilarObjectsRecommendation(objectId: String): Future[SimilarObjectsRecommendation]

  def clearAllRecommendations(): Future[Unit]
}

object RecommendationManager {
  final val IndexName: String = "recommendation"
  final val TypeName: String = "similarObjects"

  def apply(esUsername: String,
            esPassword: String,
            esClientUri: String)(implicit executionContext: ExecutionContext): RecommendationManager = {
    val esClient = ESClientProvider.createESClient(esUsername, esPassword, esClientUri)
    new RecommendationManagerImpl(esClient, IndexName, TypeName)
  }

}