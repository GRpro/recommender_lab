package lab.reco.event

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.http.ElasticClient
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.ElasticClientProvider
import lab.reco.common.model.EventConfigService
import lab.reco.event.api.Endpoints
import lab.reco.event.config.ConfigStore

object WebServer extends LazyLogging {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("event-manager")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val esClient: ElasticClient = ElasticClientProvider.createElasticClient(ConfigStore.esUsername, ConfigStore.esPassword, ConfigStore.esClientUri)

    val endpoints = new Endpoints {
      override val eventManager: EventManager = EventManager(esClient)

      override val eventConfigService: EventConfigService = EventConfigService(esClient)
    }

    val bindingFuture = Http()
      .bindAndHandle(endpoints.routes, ConfigStore.serviceHost, ConfigStore.servicePort)

    logger.info(s"running event_manager on ${ConfigStore.serviceHost}:${ConfigStore.servicePort}")

    sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate())
    }

  }
}