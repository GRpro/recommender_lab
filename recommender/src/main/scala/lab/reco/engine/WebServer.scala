package lab.reco.engine

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import lab.reco.common.RecommendationManager
import lab.reco.engine.api.Endpoints
import lab.reco.engine.config.ConfigStore

object WebServer {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("recommendation-manager")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val recommenderManager: RecommendationManager =
      RecommendationManager(ConfigStore.esUsername, ConfigStore.esPassword, ConfigStore.esClientUri)

    val endpoints = new Endpoints {
      override val manager: RecommendationManager = recommenderManager
    }

    val bindingFuture = Http()
      .bindAndHandle(endpoints.routes, ConfigStore.serviceHost, ConfigStore.servicePort)

    sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate())
    }
  }
}
