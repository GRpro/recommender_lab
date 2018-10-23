package lab.reco.common

import com.sksamuel.elastic4s.http.ElasticClient
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import lab.reco.common.util.Implicits._
import com.sksamuel.elastic4s.http.ElasticDsl._
import spray.json.{JsArray, JsString, JsonParser}
import spray.json.ParserInput.StringBasedParserInput

class RecommendationManagerImpl(esClient: ElasticClient,
                                 indexName: String,
                                 typeName: String)(implicit executionContext: ExecutionContext) extends RecommendationManager with LazyLogging {

  override def getSimilarObjectsRecommendation(objectId: String): Future[SimilarObjectsRecommendation] = {
    esClient execute get(indexName, typeName, objectId) map { result =>
      logger.info(s"get similar objects recommendation for object [$objectId], result [$result]")
      val response = result.body.get
      val obj = JsonParser(new StringBasedParserInput(response)).asJsObject
        .fields("_source").asJsObject
      val recommendations = obj
        .fields("recommendations").asInstanceOf[JsArray]
        .elements.map(_.asInstanceOf[JsString].value)
      SimilarObjectsRecommendation(objectId, recommendations)
    }
  }

  override def clearAllRecommendations(): Future[Unit] = {
      esClient execute deleteIndex(indexName) map { result =>
        logger.info(s"delete index [$indexName] result [$result]")
      } logFailure(logger, s"delete index operation [$indexName] failed")
    }
}
