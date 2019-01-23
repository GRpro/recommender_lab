package lab.reco.engine.recommendation


import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol.Recommendation._
import lab.reco.common.model.{EventConfigService, ModelConfig}
import lab.reco.common.util.Implicits._
import spray.json.{JsArray, JsObject, JsString, JsonParser}
import spray.json.ParserInput.StringBasedParserInput

import scala.concurrent.{ExecutionContext, Future}

class RecommendationManagerImpl(esClient: ElasticClient, eventConfigService: EventConfigService)(implicit executionContext: ExecutionContext) extends RecommendationManager with LazyLogging {

  final val DefaultRecommendationSize = 20

  private def orderIndicatorNames(config: ModelConfig): List[String] = {
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

  override def recommend(query: Query): Future[Recommendation] = {
    eventConfigService.getConfig()
      .filter(_.isDefined)
      .map(_.get)
      .flatMap { config =>
        val sortedIndicatorNames = orderIndicatorNames(config)
        logger.info(s"indicator names in order: $sortedIndicatorNames")

        val termsQuery = sortedIndicatorNames.flatMap { indicator =>
          query.history.getOrElse(indicator, Seq.empty).map(objectId => {
            s"""
              |        {
              |          "terms": {
              |            "${recommendationsField(indicator)}": ["$objectId"]
              |          }
              |        }""".stripMargin
          })
        }.mkString(",")

        val q =
          s"""
            |{
            |    "bool": {
            |      "should": [$termsQuery
            |      ]
            |    }
            |}
          """.stripMargin


        esClient execute {
          search(indexName)
            .limit(query.length.getOrElse(DefaultRecommendationSize))
            .rawQuery(q)

        } map { result =>
          logger.info(s"execute query [$q]")
          val response = result.body.get
          val docs = JsonParser(new StringBasedParserInput(response)).asJsObject
            .fields("hits").asJsObject.fields("hits").asInstanceOf[JsArray]

          val recommendations = docs.elements.map {
            _.asInstanceOf[JsObject].fields("_id").asInstanceOf[JsString].value
          }
          Recommendation(recommendations)
        } recover {
          case _: NoSuchElementException =>
            // no recommendations computed
            Recommendation(Seq.empty)
          case e =>
            logger.warn(s"failed to retrieve recommendations for query [$q]", e)
            throw e
        }

      }
  }
}
