package lab.reco.common.config

import com.typesafe.config.Config
import scala.util.Try

trait ConfigUtil {

  def config: Config

  def getValue[T](key: String, retriever: String => T): Option[T] =
    Try {
      retriever(key)
    }.toOption

  def getAsString(key: String): Option[String] =
    getValue(key, config.getString)

  def getAsInt(key: String): Option[Int] =
    getValue(key, config.getInt)

}
