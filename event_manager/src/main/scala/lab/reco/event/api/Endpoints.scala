package lab.reco.event.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import lab.reco.common.model.{EventConfigService, IndicatorConfig, IndicatorsConfig}
import lab.reco.event._
import spray.json._

import scala.util.{Failure, Success}


case class StoreEventResponse(sessionId: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val eventFormat: RootJsonFormat[Event] = jsonFormat5(Event)
  implicit val storeEventResponseFormat: RootJsonFormat[StoreEventResponse] = jsonFormat1(StoreEventResponse)
  implicit val indicatorConfigFormat: RootJsonFormat[IndicatorConfig] = jsonFormat2(IndicatorConfig)
  implicit val modelConfigFormat: RootJsonFormat[IndicatorsConfig] = jsonFormat2(IndicatorsConfig)
  implicit val objectGetFormat: RootJsonFormat[ObjectGet] = jsonFormat1(ObjectGet)
  implicit val objectDeleteFormat: RootJsonFormat[ObjectDelete] = jsonFormat1(ObjectDelete)

  implicit val countResultFormat: RootJsonFormat[CountResult] = jsonFormat1(CountResult)
  implicit val updateResultFormat: RootJsonFormat[UpdateResult] = jsonFormat1(UpdateResult)
  implicit val deleteResultFormat: RootJsonFormat[DeleteResult] = jsonFormat1(DeleteResult)
  implicit val objectUpdateFormat: RootJsonFormat[ObjectUpdate] = jsonFormat3(ObjectUpdate)

}

trait Endpoints extends JsonSupport {
  def eventManager: EventManager

  def eventConfigService: EventConfigService


  private def eventsApi: Route =
    path("events" / "createMany") {
      post {
        entity(as[Seq[Event]]) { events =>
          onComplete(eventManager.processEvents(events)) {
            case Success(_) => complete(StatusCodes.OK)
            case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
          }
        }
      }
    } ~
      path("events" / "createOne") {
        post {
          entity(as[Event]) { event =>
            onComplete(eventManager.processEvent(event)) {
              case Success(_) => complete(StatusCodes.OK)
              case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
        }
      } ~
      path("events" / "getByQuery") {
        post {
          entity(as[JsValue]) { event =>
            event.asJsObject.fields.get("query").map { obj =>
              val query = obj.asJsObject.compactPrint
              onComplete(eventManager.getEvents(query)) {
                case Success(events) => complete(events)
                case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
              }
            }.getOrElse {
              complete(StatusCodes.BadRequest, "invalid query")
            }
          }
        }
      } ~
      path("events" / "countByQuery") {
        post {
          entity(as[JsValue]) { event =>
            event.asJsObject.fields.get("query").map { obj =>
              val query = obj.asJsObject.compactPrint
              onComplete(eventManager.getEventsCount(query)) {
                case Success(result) => complete(CountResult(result))
                case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
              }
            }.getOrElse {
              complete(StatusCodes.BadRequest, "invalid query")
            }
          }
        }
      } ~
      path("events" / "countAll") {
        post {
          onComplete(eventManager.getAllEventsCount()) {
            case Success(result) => complete(CountResult(result))
            case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
          }
        }
      } ~
      path("events" / "deleteByQuery") {
        post {
          entity(as[JsValue]) { event =>
            event.asJsObject.fields.get("query").map { obj =>
              val query = obj.asJsObject.compactPrint
              onComplete(eventManager.deleteEvents(query)) {
                case Success(result) => complete(DeleteResult(result))
                case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
              }
            }.getOrElse {
              complete(StatusCodes.BadRequest, "invalid query")
            }
          }
        }
      } ~
      path("events" / "deleteAll") {
        post {
          onComplete(eventManager.deleteAllEvents()) {
            case Success(result) => complete(DeleteResult(result))
            case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
          }
        }
      }

  private def objectsApi: Route =
    path("objects" / "updateById") {
      post {
        entity(as[ObjectUpdate]) { objectUpdate =>
          onComplete(eventManager.updateObject(objectUpdate.objectId, objectUpdate.objectProperties, objectUpdate.replace)) {
            case Success(result) => complete(UpdateResult(result))
            case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
          }
        }
      }
    } ~
      path("objects" / "getById") {
        entity(as[ObjectGet]) { objectGet =>
          post {
            onComplete(eventManager.getObject(objectGet.objectId)) {
              case Success(properties) => complete(properties)
              case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
        }
      } ~
      path("objects" / "deleteAll") {
        post {
          onComplete(eventManager.deleteAllObjects()) {
            case Success(result) => complete(DeleteResult(result))
            case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
          }
        }
      } ~
      path("objects" / "deleteByQuery") {
        post {
          entity(as[JsValue]) { query =>
            onComplete(eventManager.deleteObjects(query.compactPrint)) {
              case Success(result) => complete(DeleteResult(result))
              case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
        }
      } ~
      path("objects" / "deleteById") {
        entity(as[ObjectDelete]) { objectDelete =>
          post {
            onComplete(eventManager.deleteObject(objectDelete.objectId)) {
              case Success(result) => complete(DeleteResult(if (result) 1 else 0))
              case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
        }
      } ~
      path("objects" / "schema") {
        get {
          onComplete(eventManager.getObjectSchema()) {
            case Success(Some(schema)) => complete(schema)
            case Success(None) => complete(StatusCodes.NotFound, "schema not found")
            case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
          }
        }
      } ~
      path("objects" / "schema") {
        post {
          entity(as[JsValue]) { query =>
            onComplete(eventManager.setObjectSchema(query.asJsObject)) {
              case Success(result) => complete(StatusCodes.OK)
              case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
        }
      }

  private def configureModelApi: Route =
    path("model") {
      get {
        onComplete(eventConfigService.getIndicatorsConfig()) {
          case Success(Some(modelConfig)) => complete(modelConfig)
          case Success(None) => complete(StatusCodes.NoContent)
          case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
        }
      }
    } ~
      path("model") {
        post { // configure model
          entity(as[IndicatorsConfig]) { modelConfig =>
            onComplete(eventConfigService.setIndicatorsConfig(modelConfig)) {
              case Success(_) => complete(StatusCodes.OK)
              case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
        }
      }


  def routes: Route =
    pathPrefix("api") {
      eventsApi ~ objectsApi ~ configureModelApi
    }
}
