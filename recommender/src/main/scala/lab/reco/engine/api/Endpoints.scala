package lab.reco.engine.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import lab.reco.engine.recommendation._
import spray.json._

import scala.util.{Failure, Success}


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val queryFormat: RootJsonFormat[Query] = jsonFormat4(Query)
  implicit val recommendationsFormat: RootJsonFormat[Recommendation] = jsonFormat3(Recommendation)
}

trait Endpoints extends JsonSupport {

  final val DefaultRecommendationsSize = 10

  def manager: RecommendationManager

  def routes: Route =
    pathPrefix("api") {
      path("recommendation") {
        post {
          entity(as[Query]) { query =>
            onComplete(manager.recommend(query)) {
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
