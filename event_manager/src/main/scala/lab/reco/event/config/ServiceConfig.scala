package lab.reco.event.config

trait ServiceConfig {

  def serviceHost: String
  def servicePort: Int
}

trait ElasticSearchClientConfig {
  def esClientUri: String
  def esUsername: String
  def esPassword: String
}

trait Config extends ServiceConfig with ElasticSearchClientConfig