package lab.reco.common.config

trait ElasticSearchClientConfig {
  def esClientUri: String
  def esUsername: String
  def esPassword: String
}