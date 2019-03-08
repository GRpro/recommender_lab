package lab.reco.engine.recommendation


import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol.Recommendation._
import lab.reco.common.model.{EventConfigService, IndicatorsConfig}
import lab.reco.common.util.Implicits._
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue, JsonParser}
import spray.json.ParserInput.StringBasedParserInput

import scala.concurrent.{ExecutionContext, Future}

class RecommendationManagerImpl(esClient: ElasticClient, eventConfigService: EventConfigService)(implicit executionContext: ExecutionContext) extends RecommendationManager with LazyLogging {

  final val DefaultRecommendationSize = 20

  private def orderIndicatorNames(config: IndicatorsConfig): List[String] = {
    // smaller number means higher priority
    config.primaryIndicator :: config.secondaryIndicators.sortBy(_.priority).map(_.name).toList
  }

  override def clearAllRecommendations(): Future[Unit] = {
    val sortedIndicatorNames =
      esClient execute deleteIndex(indexName) map { result =>
        logger.info(s"delete index [$indexName] result [$result]")
      } logFailure(logger, s"delete index operation [$indexName] failed")

    sortedIndicatorNames.map(_ => ())
  }

  override def recommend(query: Query): Future[Seq[Recommendation]] = {
    eventConfigService.getModelVersion().flatMap { modelVersion =>
      eventConfigService.getIndicatorsConfig()
        .filter(_.isDefined)
        .map(_.get)
        .flatMap { config =>
          val sortedIndicatorNames = orderIndicatorNames(config)
          logger.info(s"indicator names in order: $sortedIndicatorNames")

          val termsRecommendationsQuery = sortedIndicatorNames.flatMap { indicator =>
            // should make each term unique in the Array to boost in case more of them
            query.history.getOrElse(indicator, Seq.empty).map(objectId => {
              JsObject(Map(
                "terms" -> JsObject(Map(
                  recommendationsField(indicator, modelVersion.toString) -> JsArray(JsString(objectId))
                ))
              ))
            })
          }

          var boolQueryComponents: Map[String, JsValue] = Map.empty

          boolQueryComponents += "should" -> JsArray(termsRecommendationsQuery:_*)

          query.filter.foreach { q =>
            boolQueryComponents += "filter" -> q
          }

          query.must_not.foreach { q =>
            boolQueryComponents += "must_not" -> q
          }

          val boolQuery = JsObject(Map(
            "bool" -> JsObject(boolQueryComponents)
          ))

          val q = boolQuery.prettyPrint

          esClient execute {
            search(indexName)
              .limit(query.length.getOrElse(DefaultRecommendationSize))
              .rawQuery(q)

          } map { result =>
            logger.info(s"execute query [$q]")
            val response = result.body.get
            println(response)
            val docs = JsonParser(new StringBasedParserInput(response)).asJsObject
              .fields("hits").asJsObject.fields("hits").asInstanceOf[JsArray]

            val recommendations = docs.elements.map { obj =>
              val jsonObject = obj.asJsObject
              val objectId = jsonObject.fields("_id").asInstanceOf[JsString].value
              val fields = jsonObject.fields("_source").asJsObject.fields.get(propertiesField).map(_.asJsObject).getOrElse(JsObject.empty)
              val score = jsonObject.fields("_score").asInstanceOf[JsNumber].value.toDouble
              Recommendation(objectId, fields, score)
            }

            recommendations
          } recover {
            case _: NoSuchElementException =>
              // no recommendations computed
              Seq.empty
            case e =>
              logger.warn(s"failed to retrieve recommendations for query [$q]", e)
              throw e
          }

        }

    }
  }
}
