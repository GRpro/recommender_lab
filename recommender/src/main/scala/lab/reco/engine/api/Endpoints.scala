package lab.reco.engine.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import lab.reco.common._
import spray.json._

import scala.util.{Failure, Success}


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val similarObjectsRecommendationRequestFormat: RootJsonFormat[SimilarObjectsRecommendationRequest] = jsonFormat2(SimilarObjectsRecommendationRequest)
  implicit val similarObjectsRecommendationFormat: RootJsonFormat[SimilarObjectsRecommendation] = jsonFormat2(SimilarObjectsRecommendation)
}

trait Endpoints extends JsonSupport {
  def manager: RecommendationManager

  def routes: Route =
    pathPrefix("api") {
      path("recommendation" / "similarObjects") {
        post {
          entity(as[SimilarObjectsRecommendationRequest]) { request =>
            onComplete(manager.getSimilarObjectsRecommendation(request.objectId)) {
              case Success(res) => complete(res)
              case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
        }
      } ~
        path("recommendation") {
          delete {
            onComplete(manager.clearAllRecommendations()) {
              case Success(_) => complete(StatusCodes.OK)
              case Failure(e) => complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
        }

    }
}
