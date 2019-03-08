package lab.reco.common

import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties}
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.{HttpClientConfigCallback, RequestConfigCallback}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpec}
import pl.allegro.tech.embeddedelasticsearch.{EmbeddedElastic, PopularProperties}
import spray.json.{JsFalse, JsNumber, JsString, JsTrue, JsValue, JsonFormat}

trait ElasticSearchFixture extends BeforeAndAfterEach with BeforeAndAfterAll {
  this: WordSpec =>

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any): JsValue = x match {
      case n: Int => JsNumber(n)
      case n: Double => JsNumber(n)
      case n: Float => JsNumber(n)
      case s: String => JsString(s)
      case b: Boolean if b => JsTrue
      case b: Boolean if !b => JsFalse
    }

    def read(value: JsValue): Any = value match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
    }
  }

  def elasticInstallationDir: Path

  def transportTcpPort: Int

  def httpPort: Int

  protected final lazy val elasticClient: ElasticClient =
    createESClient(httpPort)

  private final val embeddedElastic: EmbeddedElastic = EmbeddedElastic.builder()
    .withElasticVersion("6.4.3")
    .withStartTimeout(60, TimeUnit.SECONDS)
    .withSetting(PopularProperties.TRANSPORT_TCP_PORT, transportTcpPort)
    .withSetting(PopularProperties.HTTP_PORT, httpPort)
    .withInstallationDirectory(elasticInstallationDir.toFile)
    .withSetting(PopularProperties.CLUSTER_NAME, UUID.randomUUID().toString)
    .build()

  protected override def beforeAll(): Unit =
    embeddedElastic.start()

  protected override def afterAll(): Unit =
    embeddedElastic.stop()

  private def createESClient(port: Int): ElasticClient = {
    val provider = {
      val provider = new BasicCredentialsProvider
      val credentials = new UsernamePasswordCredentials(
        "elastic",
        "elastic")
      provider.setCredentials(AuthScope.ANY, credentials)
      provider
    }

    ElasticClient(
      ElasticProperties(s"http://localhost:$port"),
      new RequestConfigCallback {
        override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder =
          requestConfigBuilder
      }, new HttpClientConfigCallback {
        override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder) =
          httpClientBuilder.setDefaultCredentialsProvider(provider)
      }
    )
  }
}
