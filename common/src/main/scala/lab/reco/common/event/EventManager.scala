package lab.reco.common.event

import com.sksamuel.elastic4s.http.ElasticClient
import lab.reco.common.Protocol

import scala.concurrent.{ExecutionContext, Future}

trait EventManager {

  def clearAllEvents(): Future[Unit]

  def storeEvent(sessionId: Option[String], event: Event): Future[String]

  def storeEvents(sessionId: Option[String], event: Seq[Event]): Future[String]

}


object EventManager {

  def apply(esClient: ElasticClient)(implicit executionContext: ExecutionContext): EventManager = {
    new EventManagerImpl(esClient, Protocol.Event.indexName, Protocol.Event.typeName)
  }

}