package lab.reco.engine.recommendation

import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol
import lab.reco.common.model.{EventConfigService, ModelConfig}
import lab.reco.common.util.Implicits._
import spray.json.{JsArray, JsString, JsonParser}
import spray.json.ParserInput.StringBasedParserInput

import scala.concurrent.{ExecutionContext, Future}

class RecommendationManagerImpl(esClient: ElasticClient, eventConfigService: EventConfigService)(implicit executionContext: ExecutionContext) extends RecommendationManager with LazyLogging {

  private val typeName = Protocol.Recommendation.typeName

  private def orderIndicatorNames(config: ModelConfig): List[String] = {
    // smaller number means higher priority
    config.primaryIndicator :: config.secondaryIndicators.sortBy(_.priority).map(_.name).toList
  }

  private def retrieveRecommendations(objectId: String, size: Int): Future[Seq[String]] = {


    def fetchMore(indicatorNames: List[String], recommendations: Seq[String], remained: Int): Future[Seq[String]] = {
      if (indicatorNames.isEmpty) {
        Future.successful(recommendations)
      } else {
        getRecommendations(indicatorNames.head, objectId).flatMap { recs =>
          val recommendationsSet = recommendations.toSet
          val fromIndicator = recs
            .take(remained)
            .filterNot(recommendationsSet(_))
          logger.info(s"fetched ${fromIndicator.size} records from indicator [${indicatorNames.head}] recommendations")
          val newRecommendations = recommendations ++ fromIndicator

          if (fromIndicator.size < remained) {
            val newRemained = remained - fromIndicator.size
            fetchMore(indicatorNames.tail, newRecommendations, newRemained)
          } else {
            Future.successful(newRecommendations)
          }
        }
      }
    }

    eventConfigService.getConfig()
      .filter(_.isDefined)
      .map(_.get)
      .flatMap { config =>
        val sortedIndicatorNames = orderIndicatorNames(config)
        logger.info(s"indicator names in order: $sortedIndicatorNames")
        fetchMore(sortedIndicatorNames, Seq.empty, size)
      }
  }


  private def getRecommendations(indexName: String, objectId: String): Future[Seq[String]] = {
    esClient execute get(indexName, typeName, objectId) map { result =>
      logger.info(s"get similar objects recommendation for object [$objectId], result [$result]")
      val response = result.body.get
      val obj = JsonParser(new StringBasedParserInput(response)).asJsObject
        .fields("_source").asJsObject
      val recommendations = obj
        .fields("recommendations").asInstanceOf[JsArray]
        .elements.map(_.asInstanceOf[JsString].value)
      recommendations
    }
  }

  override def getRecommendations(objectId: String, size: Int): Future[SimilarObjectsRecommendation] = {
    retrieveRecommendations(objectId, size).map(SimilarObjectsRecommendation(objectId, _))
  }

  override def clearAllRecommendations(): Future[Unit] = {
    eventConfigService.getConfig()
      .filter(_.isDefined)
      .map(_.get)
      .flatMap { config =>
        val sortedIndicatorNames = orderIndicatorNames(config)
        val deleteIndexResults = sortedIndicatorNames.map { name =>
          esClient execute deleteIndex(name) map { result =>
            logger.info(s"delete index [$name] result [$result]")
          } logFailure(logger, s"delete index operation [$name] failed")
        }
        Future.sequence(deleteIndexResults).map(_ => ())
      }
  }
}
