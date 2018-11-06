package lab.reco.job.config

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.config.{ConfigUtil, ElasticSearchClientConfig, ServiceConfig}

object ConfigStore extends ConfigUtil with ServiceConfig with RunnerConfig with ElasticSearchClientConfig with LazyLogging {
  override val config: Config = ConfigFactory.load().resolve()

  val serviceHost: String = getAsString("service.host").get
  val servicePort: Int = getAsInt("service.port").get

  val esClientUri: String = getAsString("service.elasticsearch.clientUri").get
  val esUsername: String = getAsString("service.elasticsearch.username").get
  val esPassword: String = getAsString("service.elasticsearch.password").get

  val exportEventsScriptPath: String = getAsString("service.exportEventsScriptPath").get
  val trainModelScriptPath: String = getAsString("service.trainModelScriptPath").get
  val exportModelScriptPath: String = getAsString("service.exportModelScriptPath").get
}
