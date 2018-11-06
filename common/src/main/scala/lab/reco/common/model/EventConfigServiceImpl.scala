package lab.reco.common.model

import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.{ArrayFieldValue, SimpleFieldValue}
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol.Metadata._
import lab.reco.common.util.Implicits._
import spray.json.ParserInput.StringBasedParserInput
import spray.json.{JsArray, JsString, JsonParser}

import scala.concurrent.{ExecutionContext, Future}

class EventConfigServiceImpl(elasticClient: ElasticClient)(implicit executionContext: ExecutionContext) extends EventConfigService with LazyLogging {

  private def indicatorConfigToString(arg: IndicatorConfig): String = s"${arg.name}-${arg.priority}"

  private def indicatorConfigFromString(confString: String): IndicatorConfig = {
    val splitted = confString.split("-")
    IndicatorConfig(splitted(0), splitted(1).toInt)
  }

  override def storeConfig(modelConfig: ModelConfig): Future[Unit] = {
    elasticClient.execute {
      indexInto(indexName, typeName) fieldValues(
        SimpleFieldValue(primaryIndicatorField, modelConfig.primaryIndicator),
        ArrayFieldValue(secondaryIndicatorsField,
          modelConfig.secondaryIndicators.map(indicatorConfig =>
            SimpleFieldValue(indicatorConfigToString(indicatorConfig))
          )
        )
      ) id modelConfigId
    }
      .logFailure(logger, "failed to store model metadata")
      .logSuccess(logger, "successfully stored model metadata")
      .map(_ => ())
  }

  override def getConfig(): Future[Option[ModelConfig]] = {
    elasticClient.execute {
      get(indexName, typeName, modelConfigId)
    }
      .map { result =>
        val response = result.body.get
        JsonParser(new StringBasedParserInput(response)).asJsObject
          .fields.get("_source").map { source =>
          val obj = source.asJsObject
          val primaryIndicator = obj.fields(primaryIndicatorField).asInstanceOf[JsString].value
          val secondaryIndicators = obj.fields.get(secondaryIndicatorsField)
            .map(_.asInstanceOf[JsArray]
              .elements
              .map(_.asInstanceOf[JsString].value)
              .map(indicatorConfigFromString)
            ).getOrElse(Seq.empty)
          ModelConfig(primaryIndicator, secondaryIndicators)
        }
      }
      .logFailure(logger, "failed to retrieve model metadata")
      .logSuccess(logger, "successfully retrieved model metadata")
  }
}
