package lab.reco.event

import java.nio.file.{Path, Paths}

import com.sksamuel.elastic4s.http.ElasticDsl.{deleteIndex, _}
import lab.reco.common.ElasticSearchFixture
import lab.reco.common.Protocol.Event.indexName
import lab.reco.common.Protocol.{Metadata, Recommendation}
import org.scalatest.{Matchers, WordSpec}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class EventManagerSpec extends WordSpec with ElasticSearchFixture with Matchers {

  private final val viewIndicator = "view"
  private final val purchaseIndicator = "purchase"

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

  private final var eventManagerService: EventManager = _

  override def beforeEach(): Unit = {
    elasticClient.execute {
      deleteIndex(indexName)
    }.await

    elasticClient.execute {
      deleteIndex(Metadata.indexName)
    }.await

    elasticClient.execute {
      deleteIndex(Recommendation.indexName)
    }.await

    eventManagerService = EventManager(elasticClient)

    Thread.sleep(1000)
  }

  "EventManager" should {

    "manage events and objects" in {

      val event1 = Event(
        subjectId = "u1",
        objectId = "i1",
        indicator = viewIndicator,
        objectProperties = None,
        timestamp = Some(1)
      )

      val event2 = Event(
        subjectId = "u1",
        objectId = "i2",
        indicator = purchaseIndicator,
        objectProperties = Some(propertiesI2.toJson.asJsObject),
        timestamp = Some(2)
      )

      val event3 = Event(
        subjectId = "u1",
        objectId = "i3",
        indicator = purchaseIndicator,
        objectProperties = Some(propertiesI3.toJson.asJsObject),
        timestamp = Some(3)
      )

      val event4 = Event(
        subjectId = "u2",
        objectId = "i4",
        indicator = viewIndicator,
        objectProperties = Some(propertiesI4.toJson.asJsObject),
        timestamp = None
      )
      eventManagerService.processEvent(event1).await
      Thread.sleep(1000)
      eventManagerService.getObjectSchema().await shouldBe None

      eventManagerService.getAllEventsCount().await shouldBe 1

      eventManagerService.processEvents(Seq(event2, event3)).await
      Thread.sleep(1000)
      eventManagerService.getObjectSchema().await should not be None

      eventManagerService.processEvent(event4).await
      Thread.sleep(1000)

      eventManagerService.getAllEventsCount().await shouldBe 4

      eventManagerService.getObject("i1").await shouldBe JsObject()
      eventManagerService.getObject("i2").await shouldBe propertiesI2.toJson.asJsObject
      eventManagerService.getObject("i3").await shouldBe propertiesI3.toJson.asJsObject
      eventManagerService.getObject("i4").await shouldBe propertiesI4.toJson.asJsObject

      eventManagerService.deleteObject("i2").await shouldBe true
      Thread.sleep(1000)
      eventManagerService.getObject("i2").await shouldBe JsObject()

      // delete by query API verify
      eventManagerService.deleteObjects(
        s"""
          |{
          |    "term" : { "properties.p2" : ${propertiesI3("p2")} }
          |}
        """.stripMargin).await shouldBe 1
      Thread.sleep(1000)

      eventManagerService.deleteObject("i10000").await shouldBe false

      eventManagerService.deleteAllObjects().await shouldBe 1

    }
  }

  override def elasticInstallationDir: Path =
    Paths.get(System.getProperty("java.io.tmpdir"), "test_elastic_instance_event_manager")
  override def transportTcpPort: Int = 9302
  override def httpPort: Int = 9202
}
