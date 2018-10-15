package lab.reco.event.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import lab.reco.common.{Event, EventManager, StoreEventResponse}
import spray.json._

import scala.util.{Failure, Success}


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val eventFormat: RootJsonFormat[Event] = jsonFormat4(Event)
  implicit val storeEventResponseFormat: RootJsonFormat[StoreEventResponse] = jsonFormat1(StoreEventResponse)
}

trait Endpoints extends JsonSupport {
  def manager: EventManager

  def routes: Route =
    pathPrefix("api") {
      path("events" / "batch") {
        post {
          parameters('sessionId.?) { sessionId =>
            entity(as[Seq[Event]]) { events =>
              onComplete(manager.storeEvents(sessionId, events)) {
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
                onComplete(manager.storeEvent(sessionId, event)) {
                  case Success(res) => complete(res)
                  case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
                }
              }
            }
          } ~
            delete {
              onComplete(manager.clearAllEvents()) {
                case Success(_) => complete(StatusCodes.OK)
                case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
              }
            }
        }

    } ~
      path("items") {
        ???
      } ~
      path("users") {
        ???
      }
}
