package lab.reco.common

import java.util.UUID

import com.sksamuel.elastic4s.SimpleFieldValue
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol.Event._
import lab.reco.common.util.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class EventManagerImpl(esClient: ElasticClient,
                       indexName: String,
                       typeName: String)(implicit executionContext: ExecutionContext) extends EventManager with LazyLogging {

  private def generateSessionId: String = UUID.randomUUID().toString

  private def currentTime: Long = System.currentTimeMillis()

  private def prepareQuery(event: Event, defaultTimestamp: => Long): IndexRequest =
    indexInto(indexName, typeName) fieldValues(
      SimpleFieldValue(subjectIdField, event.subjectId),
      SimpleFieldValue(objectIdField, event.objectId),
      SimpleFieldValue(timestampField, event.timestamp.getOrElse(defaultTimestamp)),
      SimpleFieldValue(indicatorField, event.indicator)
    )

  override def storeEvent(sessionId: Option[String], event: Event): Future[String] = {
    val id = sessionId.getOrElse(generateSessionId)

    val query = prepareQuery(event, currentTime)

    esClient execute query map { result =>
      logger.info(s"indexing result [$result]")
      id
    } logFailure(logger, "indexing failed")
  }

  override def storeEvents(sessionId: Option[String], event: Seq[Event]): Future[String] = {
    val id = sessionId.getOrElse(generateSessionId)

    lazy val time = currentTime
    val query = bulk {
      event.map(prepareQuery(_, time))
    }

    esClient execute query map { result =>
      logger.info(s"batch indexing result [$result]")
      id
    } logFailure(logger, "batch indexing failed")
  }

  override def clearAllEvents(): Future[Unit] = {
    esClient execute deleteIndex(indexName) map { result =>
      logger.info(s"delete index [$indexName] result [$result]")
    } logFailure(logger, s"delete index operation [$indexName] failed")
  }
}
