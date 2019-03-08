package lab.reco.engine.recommendation

import java.nio.file.{Path, Paths}

import com.sksamuel.elastic4s.http.ElasticDsl.{indexInto, _}
import lab.reco.common.ElasticSearchFixture
import lab.reco.common.Protocol.Metadata
import lab.reco.common.Protocol.Recommendation._
import lab.reco.common.model.{EventConfigService, IndicatorConfig, IndicatorsConfig}
import org.scalatest.{Matchers, WordSpec}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class RecommendationManagerSpec extends WordSpec with ElasticSearchFixture with Matchers {

  private final val viewIndicator = "view"
  private final val purchaseIndicator = "purchase"

  private final val modelV1 = "1"
  private final val modelV2 = "2"

  private final val propertiesI1 = Map(
    "p1" -> "val2",
    "p2" -> 3.14,
    "p3" -> false
  )

  private final val propertiesI2 = Map(
    "p1" -> "val2",
    "p2" -> 2.7,
    "p3" -> true
  )

  private final val propertiesI3 = Map(
    "p1" -> "val1",
    "p2" -> 3.14,
    "p3" -> true
  )

  private final val propertiesI4 = Map(
    "p1" -> "",
    "p2" -> 5.0
  )

  private final val propertiesI5 = Map(
    "p2" -> 5.0
  )

  private final var eventConfigService: EventConfigService = _
  private final var recommenderManager: RecommendationManager = _

  override def beforeEach(): Unit = {
    elasticClient.execute {
      deleteIndex(indexName)
    }.await

    elasticClient.execute {
      deleteIndex(Metadata.indexName)
    }.await

    elasticClient.execute {
      bulk(
        indexInto(indexName, typeName).fields(
          Map(
            "properties" -> propertiesI1,
            "objectId" -> "i1",
            recommendationsField(viewIndicator, modelV1) -> Seq("i2", "i3"),
            recommendationsField(purchaseIndicator, modelV1) -> Seq()
          )
        ).id("i1"),
        indexInto(indexName, typeName).fields {
          Map(
            "properties" -> propertiesI2,
            "objectId" -> "i2",
            recommendationsField(viewIndicator, modelV1) -> Seq("i3"),
            recommendationsField(purchaseIndicator, modelV1) -> Seq("i1", "i3")
          )
        }.id("i2"),
        indexInto(indexName, typeName).fields {
          Map(
            "properties" -> propertiesI3,
            "objectId" -> "i3",
            recommendationsField(viewIndicator, modelV1) -> Seq("i2"),
            recommendationsField(purchaseIndicator, modelV1) -> Seq("i1")
          )
        }.id("i3"),
        indexInto(indexName, typeName).fields {
          Map(
            "properties" -> propertiesI4,
            "objectId" -> "i4",
            recommendationsField(viewIndicator, modelV1) -> Seq("i5", "i1") /*,
            recommendationsField(purchaseIndicator, modelV1) -> Seq.empty */ ,
            recommendationsField(viewIndicator, modelV2) -> Seq("i1", "i3"),
            recommendationsField(purchaseIndicator, modelV2) -> Seq("i4", "i3")
          )
        }.id("i4"),
        indexInto(indexName, typeName).fields {
          Map(
            "properties" -> propertiesI5,
            "objectId" -> "i5",
            recommendationsField(viewIndicator, modelV1) -> Seq.empty,
            recommendationsField(purchaseIndicator, modelV1) -> Seq("i3", "i4"),
            recommendationsField(viewIndicator, modelV2) -> Seq("i1", "i2"),
            recommendationsField(purchaseIndicator, modelV2) -> Seq("i3")
          )
        }.id("i5")
      )
    }.await

    eventConfigService = EventConfigService(elasticClient)
    recommenderManager = RecommendationManager(elasticClient, eventConfigService)

    eventConfigService.setIndicatorsConfig(IndicatorsConfig(purchaseIndicator, Seq(IndicatorConfig(viewIndicator, 1)))).await
    eventConfigService.setModelVersion(modelV1.toInt).await

    Thread.sleep(1000)
  }

  "RecommendationManager" should {

    "rank recommended items correctly" in {

      // single indicator
      recommenderManager.recommend(Query(
        history = Map(
          viewIndicator -> Seq("i2")
        ),
        filter = None,
        must_not = None,
        length = None
      )).await shouldBe Seq(
        Recommendation("i1", propertiesI1.toJson.asJsObject, 1.0),
        Recommendation("i3", propertiesI3.toJson.asJsObject, 1.0)
      )

      // 2 indicators
      recommenderManager.recommend(Query(
        history = Map(
          viewIndicator -> Seq("i2"),
          purchaseIndicator -> Seq("i1")
        ),
        filter = None,
        must_not = None,
        length = None
      )).await shouldBe Seq(
        Recommendation("i3", propertiesI3.toJson.asJsObject, 2.0),
        Recommendation("i1", propertiesI1.toJson.asJsObject, 1.0),
        Recommendation("i2", propertiesI2.toJson.asJsObject, 1.0)
      )

      // no recommendations
      recommenderManager.recommend(Query(
        history = Map(
          viewIndicator -> Seq("i10000"),
          purchaseIndicator -> Seq("i9999")
        ),
        filter = None,
        must_not = None,
        length = None
      )).await shouldBe Seq.empty


      // switch model
      eventConfigService.setModelVersion(modelV2.toInt).await


      recommenderManager.recommend(Query(
        history = Map(
          viewIndicator -> Seq("i1", "i3"),
          purchaseIndicator -> Seq("i3")
        ),
        filter = None,
        must_not = None,
        length = None
      )).await shouldBe Seq(
        Recommendation("i4", propertiesI4.toJson.asJsObject, 3.0),
        Recommendation("i5", propertiesI5.toJson.asJsObject, 2.0)
      )
    }


    "recommend items and apply filter" in {

      recommenderManager.recommend(Query(
        history = Map(
          viewIndicator -> Seq("i2"),
          purchaseIndicator -> Seq("i1")
        ),
        filter = Some(JsObject(Map(
          "term" -> JsObject(Map(
            "properties.p1" -> JsString("val2")
          ))
        ))),
        must_not = None,
        length = None
      )).await shouldBe Seq(
        /*Recommendation("i3", propertiesI3.toJson.asJsObject, 2.0),*/
        Recommendation("i1", propertiesI1.toJson.asJsObject, 1.0),
        Recommendation("i2", propertiesI2.toJson.asJsObject, 1.0)
      )

      recommenderManager.recommend(Query(
        history = Map(
          viewIndicator -> Seq("i2"),
          purchaseIndicator -> Seq("i1")
        ),
        filter = Some(JsObject(Map(
          "term" -> JsObject(Map(
            "properties.p2" -> JsNumber(3.14)
          ))
        ))),
        must_not = None,
        length = None
      )).await shouldBe Seq(
        Recommendation("i3", propertiesI3.toJson.asJsObject, 2.0),
        Recommendation("i1", propertiesI1.toJson.asJsObject, 1.0) /*,
        Recommendation("i2", propertiesI2.toJson.asJsObject, 1.0)*/
      )

      recommenderManager.recommend(Query(
        history = Map(
          viewIndicator -> Seq("i2"),
          purchaseIndicator -> Seq("i1")
        ),
        filter = Some(JsObject(Map(
          "term" -> JsObject(Map(
            "properties.p3" -> JsBoolean(true)
          ))
        ))),
        must_not = None,
        length = None
      )).await shouldBe Seq(
        Recommendation("i3", propertiesI3.toJson.asJsObject, 2.0),
        /*Recommendation("i1", propertiesI1.toJson.asJsObject, 1.0),*/
        Recommendation("i2", propertiesI2.toJson.asJsObject, 1.0)
      )


      recommenderManager.recommend(Query(
        history = Map(
          viewIndicator -> Seq("i2"),
          purchaseIndicator -> Seq("i1")
        ),
        filter = None,
        must_not = Some(JsObject(Map(
          "term" -> JsObject(Map(
            "properties.p3" -> JsBoolean(true)
          ))
        ))),
        length = None
      )).await shouldBe Seq(/*
        Recommendation("i3", propertiesI3.toJson.asJsObject, 2.0),*/
        Recommendation("i1", propertiesI1.toJson.asJsObject, 1.0) /*,
        Recommendation("i2", propertiesI2.toJson.asJsObject, 1.0)*/
      )
    }

    "recommend items limit length" in {

      // 2 indicators
      recommenderManager.recommend(Query(
        history = Map(
          viewIndicator -> Seq("i2"),
          purchaseIndicator -> Seq("i1")
        ),
        filter = None,
        must_not = None,
        length = Some(2)
      )).await shouldBe Seq(
        Recommendation("i3", propertiesI3.toJson.asJsObject, 2.0),
        Recommendation("i1", propertiesI1.toJson.asJsObject, 1.0)
      )
    }
  }

  override def elasticInstallationDir: Path =
    Paths.get(System.getProperty("java.io.tmpdir"), "test_elastic_instance_recommender")
  override def transportTcpPort: Int = 9301
  override def httpPort: Int = 9201
}
