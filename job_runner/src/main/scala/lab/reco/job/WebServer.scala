package lab.reco.job

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.http.ElasticClient
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.ElasticClientProvider
import lab.reco.common.model.EventConfigService
import lab.reco.job.api.Endpoints
import lab.reco.job.config.ConfigStore

object WebServer extends LazyLogging {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("job-runner")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val esClient: ElasticClient = ElasticClientProvider.createElasticClient(ConfigStore.esUsername, ConfigStore.esPassword, ConfigStore.esClientUri)

    val eventManager: EventConfigService = EventConfigService(esClient)

    val endpoints = new Endpoints {
      override val modelService: ModelService = ModelService(eventManager, ConfigStore)
    }

    val bindingFuture = Http()
      .bindAndHandle(endpoints.routes, ConfigStore.serviceHost, ConfigStore.servicePort)

    logger.info(s"running job_runner on ${ConfigStore.serviceHost}:${ConfigStore.servicePort}")

    sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate())
    }

  }
}