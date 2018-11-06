package lab.reco.job.api


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import lab.reco.job.ModelService
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration._


trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

}

trait Endpoints extends JsonSupport {

  def modelService: ModelService

  def routes: Route =
    pathPrefix("api") {
      path("model" / "train") {
        post { // train model
          val result = modelService.train()
          Await.ready(result, 180.seconds) // TODO change this blocking
          complete(StatusCodes.OK)
        }
      }
    }
}
