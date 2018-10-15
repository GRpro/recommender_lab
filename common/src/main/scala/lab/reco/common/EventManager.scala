package lab.reco.common

import scala.concurrent.{ExecutionContext, Future}

trait EventManager {

  def clearAllEvents(): Future[Unit]

  def storeEvent(sessionId: Option[String], event: Event): Future[String]

  def storeEvents(sessionId: Option[String], event: Seq[Event]): Future[String]

}


object EventManager {
  final val IndexName: String = "event"
  final val TypeName: String = "indicator"

  def apply(esUsername: String,
            esPassword: String,
            esClientUri: String)(implicit executionContext: ExecutionContext): EventManager =
    new EventManagerImpl(esUsername, esPassword, esClientUri, IndexName, TypeName)
}