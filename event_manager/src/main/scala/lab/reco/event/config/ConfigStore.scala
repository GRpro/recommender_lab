package lab.reco.event.config

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try


object ConfigStore extends Config with LazyLogging {
  private val config = ConfigFactory.load().resolve()

  def getValue[T](key: String, retriever: String => T): Option[T] =
    Try {
      retriever(key)
    }.toOption

  def getAsString(key: String): Option[String] =
    getValue(key, config.getString)

  def getAsInt(key: String): Option[Int] =
    getValue(key, config.getInt)

  val serviceHost: String = getAsString("service.host").get
  val servicePort: Int = getAsInt("service.port").get

  val esClientUri: String = getAsString("service.elasticsearch.clientUri").get
  val esUsername: String = getAsString("service.elasticsearch.username").get
  val esPassword: String = getAsString("service.elasticsearch.password").get
}
