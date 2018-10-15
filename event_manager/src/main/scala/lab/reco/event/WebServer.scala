package lab.reco.event

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import lab.reco.common.EventManager
import lab.reco.event.api.Endpoints
import lab.reco.event.config.ConfigStore

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val eventManager: EventManager =
      EventManager(ConfigStore.esUsername, ConfigStore.esPassword, ConfigStore.esClientUri)

    val endpoints = new Endpoints {
      override val manager: EventManager = eventManager
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