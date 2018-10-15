package lab.reco.common

import java.util.UUID

import com.sksamuel.elastic4s.SimpleFieldValue
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.{HttpClientConfigCallback, RequestConfigCallback}
import lab.reco.common.util.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class EventManagerImpl(
                        esUsername: String,
                        esPassword: String,
                        esClientUri: String,
                        indexName: String,
                        typeName: String)(implicit executionContext: ExecutionContext) extends EventManager with LazyLogging {

  private val ESClient: ElasticClient = {
    val provider = {
      val provider = new BasicCredentialsProvider
      val credentials = new UsernamePasswordCredentials(
        esUsername,
        esPassword)
      provider.setCredentials(AuthScope.ANY, credentials)
      provider
    }

    ElasticClient(
      ElasticProperties(esClientUri),
      new RequestConfigCallback {
        override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder =
          requestConfigBuilder
      }, new HttpClientConfigCallback {
        override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder) =
          httpClientBuilder.setDefaultCredentialsProvider(provider)
      }
    )
  }

  private def generateSessionId: String = UUID.randomUUID().toString
  private def currentTime: Long = System.currentTimeMillis()

  private def prepareQuery(event: Event, defaultTimestamp: => Long): IndexRequest =
    indexInto(indexName, typeName) fieldValues(
      SimpleFieldValue("subjectId", event.subjectId),
      SimpleFieldValue("objectId", event.objectId),
      SimpleFieldValue("timestamp", event.timestamp.getOrElse(defaultTimestamp)),
      SimpleFieldValue("indicator", event.indicator)
    )

  override def storeEvent(sessionId: Option[String], event: Event): Future[String] = {
    val id = sessionId.getOrElse(generateSessionId)

    val query = prepareQuery(event, currentTime)

    ESClient execute query map { result =>
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

    ESClient execute query map { result =>
      logger.info(s"batch indexing result [$result]")
      id
    } logFailure(logger, "batch indexing failed")
  }

  override def clearAllEvents(): Future[Unit] = {
    ESClient execute deleteIndex(indexName) map { result =>
      logger.info(s"delete index [$indexName] result [$result]")
    } logFailure(logger, s"delete index operation [$indexName] failed")
  }
}
