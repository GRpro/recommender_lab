package lab.reco.event


import com.sksamuel.elastic4s.http.ElasticClient
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}


case class ObjectDelete(objectId: String)
case class ObjectGet(objectId: String)
case class ObjectUpdate(objectId: String, objectProperties: JsObject, replace: Boolean)

case class CountResult(number: Int)
case class DeleteResult(deleted: Int)
case class UpdateResult(boolean: Boolean)

/**
  *
  * @param subjectId
  * @param objectId
  * @param objectProperties optional object properties
  * @param timestamp
  * @param indicator
  */
case class Event(subjectId: String,
                 objectId: String,
                 objectProperties: Option[JsObject],
                 timestamp: Option[Long],
                 indicator: String)

case class FromElasticException(message: String) extends RuntimeException(message)


trait EventManager {

  /* events */

  /**
    * Process event which subject made towards object
    * @param event [[Event]] representing subject's action
    */
  def processEvent(event: Event): Future[Unit]

  /**
    * Process multiple events which subject made towards object
    * @param events sequence of [[Event]]
    */
  def processEvents(events: Seq[Event]): Future[Unit]

  /**
    * Delete events matching predicate
    * @param jsonQuery ElasticSearch json query
    * @return [[DeleteResult]] representing changes happened
    */
  def deleteEvents(jsonQuery: String): Future[Int]

  /**
    * Get events matching predicate
    * @param jsonQuery ElasticSearch json query
    * @return
    */
  def getEvents(jsonQuery: String): Future[Seq[Event]]

  /**
    * Delete all events
    * @return [[DeleteResult]] representing changes happened
    */
  def deleteAllEvents(): Future[Int]

  /**
    * Get number of events matching predicate
    * @param jsonQuery ElasticSearch json query
    * @return [[CountResult]] representing event count
    */
  def getEventsCount(jsonQuery: String): Future[Int]

  /**
    * Get all events count
    * @return [[CountResult]] representing event count
    */
  def getAllEventsCount(): Future[Int]

  /* object schema */

  def getObjectSchema(): Future[Option[JsObject]]

  def setObjectSchema(jsonMapping: JsObject): Future[Unit]

  /* objects */

  def getObject(objectId: String): Future[JsObject]

  def updateObject(objectId: String, objectProperties: JsObject, replace: Boolean): Future[Boolean]

  def updateObjects(updates: Seq[(String, JsObject)], replace: Boolean): Future[Unit]

  def deleteObject(objectId: String): Future[Boolean]

  def deleteObjects(jsonQuery: String): Future[Int]

  def deleteAllObjects(): Future[Int]
}


object EventManager {

  def apply(esClient: ElasticClient)
           (implicit executionContext: ExecutionContext): EventManager = new EventManagerImpl(esClient)

}