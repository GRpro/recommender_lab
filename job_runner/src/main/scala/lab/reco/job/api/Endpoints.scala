package lab.reco.job.api


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import lab.reco.job.{JobStatus, ModelService, TaskStatus}
import spray.json._

import scala.util.{Failure, Success}


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val taskStatusFormat: RootJsonFormat[TaskStatus] = jsonFormat3(TaskStatus)
  implicit val jobStatusFormat: RootJsonFormat[JobStatus] = jsonFormat5(JobStatus)
}

trait Endpoints extends JsonSupport {

  def modelService: ModelService

  def routes: Route =
    pathPrefix("api") {
      path("model" / "train") {
        post { // train model
          onComplete(modelService.train()) {
            case Success(status) =>
              complete(status)
            case Failure(e) =>
              complete(StatusCodes.BadRequest, e.getMessage)
          }
        } ~
          get { // get train model status
            modelService.getStatus match {
              case Success(status) =>
                complete(status)
              case Failure(e) =>
                complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
      }
    }
}
