package lab.reco.common

import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties}
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.{HttpClientConfigCallback, RequestConfigCallback}

import scala.concurrent.ExecutionContext

object ElasticClientProvider {

  def createElasticClient(esUsername: String,
                          esPassword: String,
                          esClientUri: String)(implicit executionContext: ExecutionContext): ElasticClient = {
    val provider = {
      val provider = new BasicCredentialsProvider
      val credentials = new UsernamePasswordCredentials(
        esUsername,
        esPassword)
      provider.setCredentials(AuthScope.ANY, credentials)
      provider
    }

    ElasticClient(
      ElasticProperties(esClientUri),
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
