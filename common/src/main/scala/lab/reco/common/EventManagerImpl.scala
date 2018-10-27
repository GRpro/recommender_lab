package lab.reco.common

import com.sksamuel.elastic4s.SimpleFieldValue
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol.Event._
import lab.reco.common.util.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class EventManagerImpl(esClient: ElasticClient,
                         indexName: String,
                         typeName: String)(implicit executionContext: ExecutionContext) extends EventManager with LazyLogging {

  createMapping()

  private def generateSessionId: String = Random.alphanumeric.take(20).mkString

  private def currentTime: Long = System.currentTimeMillis()

  private def createMapping(): Unit = {
    esClient.execute {
      createIndex(indexName) mappings (
        mapping(typeName) as (
          keywordField(sessionIdField),
          keywordField(subjectIdField),
          keywordField(objectIdField),
          longField(timestampField),
          keywordField(indicatorField)
        )
        )
    }
  }
  private def prepareQuery(sessionId: String, event: Event, defaultTimestamp: => Long): IndexRequest =
    indexInto(indexName, typeName) fieldValues(
      SimpleFieldValue(sessionIdField, sessionId),
      SimpleFieldValue(subjectIdField, event.subjectId),
      SimpleFieldValue(objectIdField, event.objectId),
      SimpleFieldValue(timestampField, event.timestamp.getOrElse(defaultTimestamp)),
      SimpleFieldValue(indicatorField, event.indicator)
    )

  override def storeEvent(sessionId: Option[String], event: Event): Future[String] = {
    val id = sessionId.getOrElse(generateSessionId)

    val query = prepareQuery(id, event, currentTime)

    esClient execute query map { result =>
      logger.info(s"indexing result [$result]")
      id
    } logFailure(logger, "indexing failed")
  }

  override def storeEvents(sessionId: Option[String], event: Seq[Event]): Future[String] = {
    val id = sessionId.getOrElse(generateSessionId)

    lazy val time = currentTime
    val query = bulk {
      event.map(prepareQuery(id, _, time))
    }

    esClient execute query map { result =>
      logger.info(s"batch indexing result [$result]")
      id
    } logFailure(logger, "batch indexing failed")
  }

  override def clearAllEvents(): Future[Unit] = {
    esClient execute deleteIndex(indexName) map { result =>
      logger.info(s"delete index [$indexName] result [$result]")
    } logFailure(logger, s"delete index operation [$indexName] failed") map { _ =>
      createMapping()
    }


  }
}
