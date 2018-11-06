package lab.reco.event.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import lab.reco.common.event.{Event, EventManager, StoreEventResponse}
import lab.reco.common.model.{IndicatorConfig, ModelConfig, EventConfigService}
import spray.json._

import scala.util.{Failure, Success}


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val eventFormat: RootJsonFormat[Event] = jsonFormat4(Event)
  implicit val storeEventResponseFormat: RootJsonFormat[StoreEventResponse] = jsonFormat1(StoreEventResponse)
  implicit val indicatorConfigFormat: RootJsonFormat[IndicatorConfig] = jsonFormat2(IndicatorConfig)
  implicit val modelConfigFormat: RootJsonFormat[ModelConfig] = jsonFormat2(ModelConfig)
}

trait Endpoints extends JsonSupport {
  def eventManager: EventManager

  def eventConfigService: EventConfigService

  def routes: Route =
    pathPrefix("api") {
      path("events" / "batch") {
        post {
          parameters('sessionId.?) { sessionId =>
            entity(as[Seq[Event]]) { events =>
              onComplete(eventManager.storeEvents(sessionId, events)) {
                case Success(res) => complete(res)
                case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
              }
            }
          }
        }
      } ~
        path("events") {
          post {
            parameters('sessionId.?) { sessionId =>
              entity(as[Event]) { event =>
                onComplete(eventManager.storeEvent(sessionId, event)) {
                  case Success(res) => complete(res)
                  case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
                }
              }
            }
          } ~
            delete {
              onComplete(eventManager.clearAllEvents()) {
                case Success(_) => complete(StatusCodes.OK)
                case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
              }
            }
        } ~
        path("model") {
          get {
            onComplete(eventConfigService.getConfig()) {
              case Success(Some(modelConfig)) => complete(modelConfig)
              case Success(None) => complete(StatusCodes.NoContent)
              case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
            }
          } ~
            post { // configure model
              entity(as[ModelConfig]) { modelConfig =>
                onComplete(eventConfigService.storeConfig(modelConfig)) {
                  case Success(_) => complete(StatusCodes.OK)
                  case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
                }
              }
            }
        }
    }
}
