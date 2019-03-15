package lab.reco.engine

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.http.ElasticClient
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.ElasticClientProvider
import lab.reco.common.model.EventConfigService
import lab.reco.engine.recommendation.RecommendationManager
import lab.reco.engine.api.Endpoints
import lab.reco.engine.config.ConfigStore

object WebServer extends LazyLogging {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("recommendation-manager")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val esClient: ElasticClient = ElasticClientProvider.createElasticClient(ConfigStore.esUsername, ConfigStore.esPassword, ConfigStore.esClientUri)
    val eventConfigService: EventConfigService = EventConfigService(esClient)

    val recommenderManager: RecommendationManager = RecommendationManager(esClient, eventConfigService)

    val endpoints = new Endpoints {
      override val manager: RecommendationManager = recommenderManager
    }

    val bindingFuture = Http()
      .bindAndHandle(endpoints.routes, ConfigStore.serviceHost, ConfigStore.servicePort)

    logger.info(s"running recommender on ${ConfigStore.serviceHost}:${ConfigStore.servicePort}")

    sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate())
    }
  }
}
